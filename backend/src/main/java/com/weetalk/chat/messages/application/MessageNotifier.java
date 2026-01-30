package com.weetalk.chat.messages.application;

import com.weetalk.chat.accounts.domain.User;
import com.weetalk.chat.accounts.infrastructure.UserRepository;
import com.weetalk.chat.children.domain.Child;
import com.weetalk.chat.children.infrastructure.ChildRepository;
import com.weetalk.chat.messages.api.dto.MessageItemResponse;
import com.weetalk.chat.messages.infrastructure.mongo.MessageDoc;
import com.weetalk.chat.moderation.domain.ModerationDecision;
import com.weetalk.chat.moderation.domain.ModerationStatus;
import com.weetalk.chat.threads.infrastructure.mongo.ThreadDoc;
import com.weetalk.chat.threads.infrastructure.mongo.ThreadMember;
import java.util.Optional;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageNotifier {
	private final UserRepository userRepository;
	private final ChildRepository childRepository;
	private final SimpMessagingTemplate messagingTemplate;

	public MessageNotifier(
		UserRepository userRepository,
		ChildRepository childRepository,
		SimpMessagingTemplate messagingTemplate
	) {
		this.userRepository = userRepository;
		this.childRepository = childRepository;
		this.messagingTemplate = messagingTemplate;
	}

	public void pushMessageToMembers(ThreadDoc thread, MessageDoc message) {
		MessageItemResponse payload = new MessageItemResponse(
			message.getId(),
			message.getThreadId(),
			message.getSenderAccountId(),
			message.getText(),
			message.getCreatedAt()
		);
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

	private boolean isApprovedForChild(MessageDoc message) {
		ModerationDecision decision = message.getModerationDecision();
		if (decision == null) {
			return false;
		}
		return decision.getStatus() == ModerationStatus.APPROVED;
	}
}
