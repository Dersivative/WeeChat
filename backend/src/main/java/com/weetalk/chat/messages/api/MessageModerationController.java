package com.weetalk.chat.messages.api;

import com.weetalk.chat.auth.security.AuthUserPrincipal;
import com.weetalk.chat.messages.api.dto.MessageItemResponse;
import com.weetalk.chat.messages.application.ThreadMessageService;
import java.security.Principal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageModerationController {
	private final ThreadMessageService threadMessageService;

	public MessageModerationController(ThreadMessageService threadMessageService) {
		this.threadMessageService = threadMessageService;
	}

	@PostMapping("/{messageId}/approve")
	public MessageItemResponse approveMessage(@PathVariable String messageId, Principal principal) {
		AuthUserPrincipal user = extractPrincipal(principal);
		return threadMessageService.approveMessage(user.getAccountId(), messageId);
	}

	private AuthUserPrincipal extractPrincipal(Principal principal) {
		if (principal instanceof UsernamePasswordAuthenticationToken authentication
			&& authentication.getPrincipal() instanceof AuthUserPrincipal userPrincipal) {
			return userPrincipal;
		}
		throw new IllegalStateException("Missing authenticated user");
	}
}
