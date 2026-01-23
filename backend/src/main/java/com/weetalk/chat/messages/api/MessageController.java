package com.weetalk.chat.messages.api;

import com.weetalk.chat.auth.security.AuthUserPrincipal;
import com.weetalk.chat.messages.api.dto.MessageItemResponse;
import com.weetalk.chat.messages.api.dto.MessageListResponse;
import com.weetalk.chat.messages.api.dto.SendMessageRequest;
import com.weetalk.chat.messages.application.ThreadMessageService;
import java.security.Principal;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/threads/{threadId}/messages")
public class MessageController {
	private final ThreadMessageService threadMessageService;

	public MessageController(ThreadMessageService threadMessageService) {
		this.threadMessageService = threadMessageService;
	}

	@GetMapping
	public MessageListResponse listMessages(
		@PathVariable String threadId,
		@RequestParam(defaultValue = "10") int limit,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
		Principal principal
	) {
		AuthUserPrincipal user = extractPrincipal(principal);
		return threadMessageService.listMessages(user.getAccountId(), threadId, before, limit);
	}

	@PostMapping
	public MessageItemResponse sendMessage(
		@PathVariable String threadId,
		@RequestBody SendMessageRequest request,
		Principal principal
	) {
		AuthUserPrincipal user = extractPrincipal(principal);
		return threadMessageService.sendMessage(user.getAccountId(), threadId, request.getText());
	}

	private AuthUserPrincipal extractPrincipal(Principal principal) {
		if (principal instanceof UsernamePasswordAuthenticationToken authentication
			&& authentication.getPrincipal() instanceof AuthUserPrincipal userPrincipal) {
			return userPrincipal;
		}
		throw new IllegalStateException("Missing authenticated user");
	}
}
