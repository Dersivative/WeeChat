package com.weetalk.chat.thread;

import com.weetalk.chat.auth.AuthUserPrincipal;
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
