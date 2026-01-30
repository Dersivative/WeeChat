package com.weetalk.chat.moderation.application;

import com.weetalk.chat.messages.application.MessageNotifier;
import com.weetalk.chat.messages.infrastructure.mongo.MessageDoc;
import com.weetalk.chat.messages.infrastructure.mongo.MessageRepository;
import com.weetalk.chat.moderation.domain.ModerationDecision;
import com.weetalk.chat.moderation.domain.ModerationStatus;
import com.weetalk.chat.moderation.infrastructure.PlGuardModerationClient;
import com.weetalk.chat.moderation.infrastructure.PlGuardModerationClient.PlGuardResult;
import com.weetalk.chat.threads.infrastructure.mongo.ThreadDoc;
import com.weetalk.chat.threads.infrastructure.mongo.ThreadRepository;
import com.weetalk.chat.children.domain.ModerationLevel;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ModerationLlmService {
	private static final String SAFE_LABEL = "safe";
	private static final Logger logger = LoggerFactory.getLogger(ModerationLlmService.class);

	private final PlGuardModerationClient plGuardModerationClient;
	private final MessageRepository messageRepository;
	private final ThreadRepository threadRepository;
	private final MessageNotifier messageNotifier;

	public ModerationLlmService(
		PlGuardModerationClient plGuardModerationClient,
		MessageRepository messageRepository,
		ThreadRepository threadRepository,
		MessageNotifier messageNotifier
	) {
		this.plGuardModerationClient = plGuardModerationClient;
		this.messageRepository = messageRepository;
		this.threadRepository = threadRepository;
		this.messageNotifier = messageNotifier;
	}

	@Async("moderationTaskExecutor")
	public void requestModeration(String messageId, ModerationLevel level) {
		MessageDoc message = messageRepository.findById(messageId).orElse(null);
		if (message == null || message.getText() == null) {
			return;
		}
		ThreadDoc thread = threadRepository.findById(message.getThreadId()).orElse(null);
		if (thread == null) {
			return;
		}

		ModerationDecision decision = message.getModerationDecision();
		if (decision == null) {
			decision = new ModerationDecision();
		}
		Map<String, Object> modelData = new LinkedHashMap<>();
		ModerationStatus suggested;
		boolean isSafe;
		try {
			PlGuardResult result = plGuardModerationClient.moderate(message.getText());
			isSafe = SAFE_LABEL.equalsIgnoreCase(result.label());
			suggested = isSafe ? ModerationStatus.APPROVED : ModerationStatus.REJECTED;
			modelData.put("label", result.label());
			modelData.put("score", result.score());
			modelData.put("suggestedStatus", suggested.name());
		} catch (Exception ex) {
			logger.warn("LLM moderation failed for messageId={}", messageId, ex);
			suggested = ModerationStatus.PENDING;
			isSafe = false;
			modelData.put("error", "LLM request failed");
		}
		decision.setModelData(modelData);

		if (level == ModerationLevel.AUTOMATED) {
			if (suggested == ModerationStatus.APPROVED || suggested == ModerationStatus.REJECTED) {
				decision.setStatus(suggested);
				decision.setDecidedAt(Instant.now());
				decision.setDecidedByParentUsername("system");
				decision.setReason(isSafe ? "Auto-approved" : "Auto-rejected");
			} else {
				decision.setStatus(ModerationStatus.PENDING);
				decision.setReason("Automated moderation");
			}
		} else if (level == ModerationLevel.MANUAL) {
			decision.setStatus(ModerationStatus.PENDING);
			decision.setReason("Manual moderation");
		}

		message.setModerationDecision(decision);
		MessageDoc saved = messageRepository.save(message);
		if (level == ModerationLevel.AUTOMATED && decision.getStatus() == ModerationStatus.APPROVED) {
			messageNotifier.pushMessageToMembers(thread, saved);
		}
	}
}
