package com.weetalk.chat.messages.application;

import com.weetalk.chat.messages.api.dto.MessageItemResponse;
import com.weetalk.chat.messages.api.dto.MessageListResponse;
import com.weetalk.chat.messages.infrastructure.mongo.MessageDoc;
import com.weetalk.chat.messages.infrastructure.mongo.MessageRepository;
import com.weetalk.chat.moderation.domain.ModerationDecision;
import com.weetalk.chat.moderation.domain.ModerationStatus;
import com.weetalk.chat.accounts.domain.User;
import com.weetalk.chat.accounts.infrastructure.UserRepository;
import com.weetalk.chat.children.domain.Child;
import com.weetalk.chat.children.infrastructure.ChildRepository;
import com.weetalk.chat.threads.infrastructure.mongo.ThreadDoc;
import com.weetalk.chat.threads.infrastructure.mongo.ThreadMember;
import com.weetalk.chat.threads.infrastructure.mongo.ThreadRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ThreadMessageService {
	private static final int MAX_PAGE_SIZE = 50;
	private static final String MESSAGE_UNAVAILABLE = "Message unavailable";

	private final MessageRepository messageRepository;
	private final ThreadRepository threadRepository;
	private final ChildRepository childRepository;
	private final UserRepository userRepository;
	private final SimpMessagingTemplate messagingTemplate;

	public ThreadMessageService(
		MessageRepository messageRepository,
		ThreadRepository threadRepository,
		ChildRepository childRepository,
		UserRepository userRepository,
		SimpMessagingTemplate messagingTemplate
	) {
		this.messageRepository = messageRepository;
		this.threadRepository = threadRepository;
		this.childRepository = childRepository;
		this.userRepository = userRepository;
		this.messagingTemplate = messagingTemplate;
	}

	public MessageListResponse listMessages(UUID viewerAccountId, String threadId, Instant before, int limit) {
		requireThreadAccess(viewerAccountId, threadId);
		int pageSize = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
		boolean isChild = viewerAccountId != null && childRepository.existsById(viewerAccountId);
		MessagePage messagePage = isChild
			? loadApprovedMessages(threadId, before, pageSize)
			: loadMessages(threadId, before, pageSize);

		List<MessageItemResponse> messages = messagePage.docs
			.stream()
			.sorted(Comparator.comparing(MessageDoc::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
			.map(this::toResponse)
			.toList();

		return new MessageListResponse(messages, messagePage.hasMore, messagePage.nextBefore);
	}

	public MessageItemResponse sendMessage(UUID senderAccountId, String threadId, String text) {
		ThreadDoc thread = requireThreadAccess(senderAccountId, threadId);
		if (text == null || text.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message text cannot be empty");
		}

		Instant now = Instant.now();
		MessageDoc message = new MessageDoc();
		message.setThreadId(threadId);
		message.setSenderAccountId(senderAccountId);
		message.setText(text.trim());
		message.setCreatedAt(now);
		if (message.getModerationDecision() == null) {
			message.setModerationDecision(new ModerationDecision());
		}

		MessageDoc saved = messageRepository.save(message);
		thread.setLastMessageAt(now);
		threadRepository.save(thread);
		pushMessageToMembers(thread, saved);

		return toResponse(saved);
	}

	public MessageItemResponse approveMessage(UUID approverAccountId, String messageId) {
		MessageDoc message = messageRepository.findById(messageId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
		ThreadDoc thread = requireThreadAccess(approverAccountId, message.getThreadId());

		ModerationDecision decision = message.getModerationDecision();
		if (decision == null) {
			decision = new ModerationDecision();
		}
		decision.setStatus(ModerationStatus.APPROVED);
		decision.setDecidedAt(Instant.now());
		decision.setDecidedByParentUsername(resolveApproverUsername(approverAccountId));
		message.setModerationDecision(decision);

		MessageDoc saved = messageRepository.save(message);
		pushMessageToMembers(thread, saved);
		return toResponse(saved);
	}

	private ThreadDoc requireThreadAccess(UUID accountId, String threadId) {
		ThreadDoc thread = threadRepository.findById(threadId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));

		boolean isMember = thread.getMembers()
			.stream()
			.anyMatch(member -> isActiveMember(member, accountId));

		if (!isMember) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a thread member");
		}

		return thread;
	}

	private boolean isActiveMember(ThreadMember member, UUID accountId) {
		if (member.getLeftAt() != null) {
			return false;
		}
		return accountId != null && accountId.equals(member.getAccountId());
	}

	private MessagePage loadMessages(String threadId, Instant before, int pageSize) {
		PageRequest pageRequest = PageRequest.of(0, pageSize);
		Page<MessageDoc> page = before == null
			? messageRepository.findByThreadIdOrderByCreatedAtDesc(threadId, pageRequest)
			: messageRepository.findByThreadIdAndCreatedAtBeforeOrderByCreatedAtDesc(threadId, before, pageRequest);

		List<MessageDoc> docs = page.getContent();
		Instant nextBefore = docs.isEmpty() ? null : docs.get(docs.size() - 1).getCreatedAt();
		return new MessagePage(docs, page.hasNext(), nextBefore);
	}

	private MessagePage loadApprovedMessages(String threadId, Instant before, int pageSize) {
		List<MessageDoc> approved = new ArrayList<>();
		boolean hasMore = false;
		Instant cursor = before;

		while (approved.size() < pageSize) {
			PageRequest pageRequest = PageRequest.of(0, pageSize);
			Page<MessageDoc> page = cursor == null
				? messageRepository.findByThreadIdOrderByCreatedAtDesc(threadId, pageRequest)
				: messageRepository.findByThreadIdAndCreatedAtBeforeOrderByCreatedAtDesc(threadId, cursor, pageRequest);

			List<MessageDoc> docs = page.getContent();
			if (docs.isEmpty()) {
				hasMore = false;
				break;
			}

			approved.addAll(docs.stream().filter(this::isApprovedForChild).toList());
			MessageDoc lastDoc = docs.get(docs.size() - 1);
			cursor = lastDoc.getCreatedAt();
			hasMore = page.hasNext();
			if (!hasMore || cursor == null) {
				break;
			}
		}

		if (approved.size() > pageSize) {
			approved = approved.subList(0, pageSize);
			hasMore = true;
		}

		Instant nextBefore = approved.isEmpty()
			? null
			: approved.get(approved.size() - 1).getCreatedAt();

		return new MessagePage(approved, hasMore, nextBefore);
	}

	private MessageItemResponse toResponse(MessageDoc message) {
		String text = resolveMessageText(message);
		return new MessageItemResponse(
			message.getId(),
			message.getThreadId(),
			message.getSenderAccountId(),
			text,
			message.getCreatedAt()
		);
	}

	private String resolveMessageText(MessageDoc message) {
		if (message.getDeletedAt() != null) {
			return MESSAGE_UNAVAILABLE;
		}
		if (message.getText() == null || message.getText().isBlank()) {
			return MESSAGE_UNAVAILABLE;
		}
		return message.getText();
	}

	private boolean isApprovedForChild(MessageDoc message) {
		ModerationDecision decision = message.getModerationDecision();
		if (decision == null) {
			return false;
		}
		return decision.getStatus() == ModerationStatus.APPROVED;
	}

	private void pushMessageToMembers(ThreadDoc thread, MessageDoc message) {
		MessageItemResponse payload = toResponse(message);
		for (ThreadMember member : thread.getMembers()) {
			if (member.getLeftAt() != null) {
				continue;
			}
			UUID accountId = member.getAccountId();
			if (accountId == null) {
				continue;
			}
			Optional<User> user = userRepository.findById(accountId);
			if (user.isPresent()) {
				messagingTemplate.convertAndSendToUser(user.get().getLogin(), "/queue/messages", payload);
				continue;
			}
			Optional<Child> child = childRepository.findById(accountId);
			if (child.isEmpty()) {
				continue;
			}
			if (!isApprovedForChild(message)) {
				continue;
			}
			messagingTemplate.convertAndSendToUser(child.get().getDisplayName(), "/queue/messages", payload);
		}
	}

	private String resolveApproverUsername(UUID approverAccountId) {
		return userRepository.findById(approverAccountId)
			.map(User::getLogin)
			.or(() -> childRepository.findById(approverAccountId).map(Child::getDisplayName))
			.orElse(null);
	}

	private record MessagePage(List<MessageDoc> docs, boolean hasMore, Instant nextBefore) {
	}
}
