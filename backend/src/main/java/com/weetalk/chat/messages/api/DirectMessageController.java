package com.weetalk.chat.messages.api;

import com.weetalk.chat.auth.security.AuthUserPrincipal;
import com.weetalk.chat.messages.api.dto.MessageItemResponse;
import com.weetalk.chat.messages.api.dto.SendDirectMessageRequest;
import com.weetalk.chat.messages.application.ThreadMessageService;
import java.security.Principal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/threads")
public class DirectMessageController {
	private final ThreadMessageService threadMessageService;

	public DirectMessageController(ThreadMessageService threadMessageService) {
		this.threadMessageService = threadMessageService;
	}

	@PostMapping("/direct/messages")
	public MessageItemResponse sendDirectMessage(@RequestBody SendDirectMessageRequest request, Principal principal) {
		AuthUserPrincipal user = extractPrincipal(principal);
		return threadMessageService.sendDirectMessage(user.getAccountId(), request.getRecipientAccountId(), request.getText());
	}

	private AuthUserPrincipal extractPrincipal(Principal principal) {
		if (principal instanceof UsernamePasswordAuthenticationToken authentication
			&& authentication.getPrincipal() instanceof AuthUserPrincipal userPrincipal) {
			return userPrincipal;
		}
		throw new IllegalStateException("Missing authenticated user");
	}
}
