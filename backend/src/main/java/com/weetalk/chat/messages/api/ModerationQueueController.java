package com.weetalk.chat.messages.api;

import com.weetalk.chat.auth.security.AuthUserPrincipal;
import com.weetalk.chat.messages.api.dto.ModerationQueueResponse;
import com.weetalk.chat.messages.application.ModerationQueueService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/children/{childId}/moderation-queue")
public class ModerationQueueController {
	private final ModerationQueueService moderationQueueService;

	public ModerationQueueController(ModerationQueueService moderationQueueService) {
		this.moderationQueueService = moderationQueueService;
	}

	@GetMapping
	public ModerationQueueResponse loadQueue(
		@AuthenticationPrincipal AuthUserPrincipal principal,
		@PathVariable UUID childId
	) {
		return moderationQueueService.loadQueue(principal.getAccountId(), childId);
	}
}
