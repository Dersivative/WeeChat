package com.weetalk.chat.threads.api;

import com.weetalk.chat.auth.security.AuthUserPrincipal;
import com.weetalk.chat.threads.api.dto.ThreadListResponse;
import com.weetalk.chat.threads.application.ThreadListService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/threads")
public class ThreadController {
	private final ThreadListService threadListService;

	public ThreadController(ThreadListService threadListService) {
		this.threadListService = threadListService;
	}

	@GetMapping
	public ResponseEntity<ThreadListResponse> listThreads(
		@AuthenticationPrincipal AuthUserPrincipal principal
	) {
		return ResponseEntity.ok(threadListService.listThreads(principal.getAccountId()));
	}
}
