package com.weetalk.chat.messages.application;

import com.weetalk.chat.accounts.domain.Account;
import com.weetalk.chat.accounts.domain.User;
import com.weetalk.chat.accounts.infrastructure.AccountRepository;
import com.weetalk.chat.accounts.infrastructure.UserRepository;
import com.weetalk.chat.children.domain.Child;
import com.weetalk.chat.children.infrastructure.ChildRepository;
import com.weetalk.chat.media.MediaUrlResolver;
import com.weetalk.chat.messages.api.dto.ModerationMessageResponse;
import com.weetalk.chat.messages.api.dto.ModerationQueueResponse;
import com.weetalk.chat.messages.api.dto.ModerationThreadResponse;
import com.weetalk.chat.messages.infrastructure.mongo.MessageDoc;
import com.weetalk.chat.messages.infrastructure.mongo.MessageRepository;
import com.weetalk.chat.moderation.domain.ModerationDecision;
import com.weetalk.chat.moderation.domain.ModerationStatus;
import com.weetalk.chat.threads.infrastructure.mongo.ThreadDoc;
import com.weetalk.chat.threads.infrastructure.mongo.ThreadMember;
import com.weetalk.chat.threads.infrastructure.mongo.ThreadRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ModerationQueueService {
	private static final int DEFAULT_THREAD_LIMIT = 25;
	private static final int DEFAULT_MESSAGE_LIMIT = 40;

	private final UserRepository userRepository;
	private final ChildRepository childRepository;
	private final ThreadRepository threadRepository;
	private final MessageRepository messageRepository;
	private final AccountRepository accountRepository;
	private final MediaUrlResolver mediaUrlResolver;

	public ModerationQueueService(
		UserRepository userRepository,
		ChildRepository childRepository,
		ThreadRepository threadRepository,
		MessageRepository messageRepository,
		AccountRepository accountRepository,
		MediaUrlResolver mediaUrlResolver
	) {
		this.userRepository = userRepository;
		this.childRepository = childRepository;
		this.threadRepository = threadRepository;
		this.messageRepository = messageRepository;
		this.accountRepository = accountRepository;
		this.mediaUrlResolver = mediaUrlResolver;
	}

	public ModerationQueueResponse loadQueue(UUID parentId, UUID childId) {
		User parent = loadParent(parentId);
		Child child = loadChildForParent(parent, childId);

		List<ThreadDoc> threads = threadRepository
			.findByMembersAccountIdAndMembersLeftAtIsNullOrderByLastMessageAtDesc(child.getId());
		if (threads.size() > DEFAULT_THREAD_LIMIT) {
			threads = threads.subList(0, DEFAULT_THREAD_LIMIT);
		}

		Map<UUID, Account> accounts = loadAccounts(threads);

		List<ModerationThreadResponse> responses = new ArrayList<>();
		for (ThreadDoc thread : threads) {
			List<MessageDoc> messages = messageRepository
				.findByThreadIdOrderByCreatedAtDesc(thread.getId(), PageRequest.of(0, DEFAULT_MESSAGE_LIMIT))
				.getContent();

			List<ModerationMessageResponse> threadMessages = messages.stream()
				.map(this::toModerationResponse)
				.sorted(Comparator.comparing(ModerationMessageResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
				.toList();

			if (threadMessages.isEmpty()) {
				continue;
			}

			String title = resolveTitle(thread, accounts, child.getId());
			List<String> avatarUrls = resolveAvatars(thread, accounts, child.getId());
			Instant lastMessageAt = threadMessages.get(threadMessages.size() - 1).getCreatedAt();

			responses.add(new ModerationThreadResponse(
				thread.getId(),
				title,
				lastMessageAt,
				avatarUrls,
				threadMessages
			));
		}

		responses.sort(Comparator.comparing(ModerationThreadResponse::getLastMessageAt, Comparator.nullsLast(Comparator.naturalOrder()))
			.reversed());

		return new ModerationQueueResponse(responses);
	}

	private User loadParent(UUID parentId) {
		return userRepository.findById(parentId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Parent account required"));
	}

	private Child loadChildForParent(User parent, UUID childId) {
		Child child = childRepository.findById(childId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
		boolean linked = parent.getChildren()
			.stream()
			.anyMatch(parentChild -> childId.equals(parentChild.getId()));
		if (!linked) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Parent not linked to child");
		}
		return child;
	}

	private Map<UUID, Account> loadAccounts(List<ThreadDoc> threads) {
		List<UUID> accountIds = threads.stream()
			.flatMap(thread -> thread.getMembers().stream())
			.map(ThreadMember::getAccountId)
			.filter(Objects::nonNull)
			.distinct()
			.toList();
		return accountRepository.findAllById(accountIds)
			.stream()
			.collect(Collectors.toMap(Account::getId, Function.identity()));
	}

	private ModerationMessageResponse toModerationResponse(MessageDoc message) {
		ModerationDecision decision = message.getModerationDecision();
		ModerationStatus status = decision == null ? ModerationStatus.PENDING : decision.getStatus();
		ModerationStatus suggested = null;
		Double score = null;
		String label = null;
		if (decision != null && decision.getModelData() != null) {
			Object suggestedValue = decision.getModelData().get("suggestedStatus");
			if (suggestedValue instanceof String suggestedStatus) {
				try {
					suggested = ModerationStatus.valueOf(suggestedStatus);
				} catch (IllegalArgumentException ignored) {
					suggested = null;
				}
			}
			Object scoreValue = decision.getModelData().get("score");
			if (scoreValue instanceof Number scoreNumber) {
				score = scoreNumber.doubleValue();
			}
			Object labelValue = decision.getModelData().get("label");
			if (labelValue instanceof String labelText) {
				label = labelText;
			}
		}
		return new ModerationMessageResponse(
			message.getId(),
			message.getThreadId(),
			message.getSenderAccountId(),
			message.getText(),
			message.getCreatedAt(),
			status,
			suggested,
			score,
			label
		);
	}

	private String resolveTitle(ThreadDoc thread, Map<UUID, Account> accounts, UUID viewerAccountId) {
		if (thread.getCustomTitle() != null && !thread.getCustomTitle().isBlank()) {
			return thread.getCustomTitle();
		}
		List<ThreadMember> members = thread.getMembers()
			.stream()
			.filter(member -> member.getLeftAt() == null)
			.toList();
		if (members.size() == 2) {
			for (ThreadMember member : members) {
				if (!viewerAccountId.equals(member.getAccountId())) {
					return displayNameFor(accounts, member.getAccountId());
				}
			}
		}
		if (!members.isEmpty()) {
			ThreadMember firstMember = members.get(0);
			int remaining = Math.max(0, members.size() - 2);
			if (members.size() >= 3) {
				return displayNameFor(accounts, firstMember.getAccountId()) + " + " + remaining + " osób";
			}
			return displayNameFor(accounts, firstMember.getAccountId());
		}
		return "Chat";
	}

	private List<String> resolveAvatars(ThreadDoc thread, Map<UUID, Account> accounts, UUID viewerAccountId) {
		List<ThreadMember> members = thread.getMembers()
			.stream()
			.filter(member -> member.getLeftAt() == null)
			.toList();
		List<String> avatars = new ArrayList<>();
		if (members.size() == 2) {
			for (ThreadMember member : members) {
				if (!viewerAccountId.equals(member.getAccountId())) {
					String avatarUrl = avatarFor(accounts, member.getAccountId());
					if (avatarUrl != null) {
						avatars.add(avatarUrl);
					}
					return avatars;
				}
			}
		}
		for (int i = 0; i < Math.min(3, members.size()); i += 1) {
			String avatarUrl = avatarFor(accounts, members.get(i).getAccountId());
			if (avatarUrl != null) {
				avatars.add(avatarUrl);
			}
		}
		return avatars;
	}

	private String displayNameFor(Map<UUID, Account> accounts, UUID accountId) {
		Account account = accounts.get(accountId);
		if (account != null && account.getDisplayName() != null && !account.getDisplayName().isBlank()) {
			return account.getDisplayName();
		}
		return "Unknown";
	}

	private String avatarFor(Map<UUID, Account> accounts, UUID accountId) {
		Account account = accounts.get(accountId);
		if (account != null && account.getAvatarFileName() != null && !account.getAvatarFileName().isBlank()) {
			return mediaUrlResolver.resolveAvatarUrl(account.getAvatarFileName());
		}
		return null;
	}
}
