package com.weetalk.chat.threads.api;

import com.weetalk.chat.auth.security.AuthUserPrincipal;
import com.weetalk.chat.threads.api.dto.ThreadListResponse;
import com.weetalk.chat.threads.application.ThreadListService;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

@Controller
public class ThreadWsController {
	private final ThreadListService threadListService;

	public ThreadWsController(ThreadListService threadListService) {
		this.threadListService = threadListService;
	}

	@MessageMapping("/threads/list")
	@SendToUser("/queue/threads")
	public ThreadListResponse listThreads(Principal principal) {
		AuthUserPrincipal user = extractPrincipal(principal);
		return threadListService.listThreads(user.getAccountId());
	}

	private AuthUserPrincipal extractPrincipal(Principal principal) {
		if (principal instanceof UsernamePasswordAuthenticationToken authentication
			&& authentication.getPrincipal() instanceof AuthUserPrincipal userPrincipal) {
			return userPrincipal;
		}
		throw new IllegalStateException("Missing authenticated user");
	}
}
