package com.weetalk.chat.accounts.api;

import com.weetalk.chat.accounts.api.dto.FriendListItemResponse;
import com.weetalk.chat.accounts.api.dto.FriendRequestCreateRequest;
import com.weetalk.chat.accounts.api.dto.FriendRequestResponse;
import com.weetalk.chat.accounts.api.dto.FriendSearchResponse;
import com.weetalk.chat.accounts.application.FriendshipService;
import com.weetalk.chat.auth.security.AuthUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {
	private final FriendshipService friendshipService;

	public FriendshipController(FriendshipService friendshipService) {
		this.friendshipService = friendshipService;
	}

	@GetMapping
	public ResponseEntity<List<FriendListItemResponse>> listFriends(
		@AuthenticationPrincipal AuthUserPrincipal principal
	) {
		return ResponseEntity.ok(friendshipService.listFriends(principal.getAccountId()));
	}

	@GetMapping("/search")
	public ResponseEntity<FriendSearchResponse> searchByEmail(
		@AuthenticationPrincipal AuthUserPrincipal principal,
		@RequestParam String email
	) {
		return ResponseEntity.ok(friendshipService.searchByEmail(principal.getAccountId(), email));
	}

	@PostMapping("/requests")
	public ResponseEntity<FriendRequestResponse> createRequest(
		@AuthenticationPrincipal AuthUserPrincipal principal,
		@Valid @RequestBody FriendRequestCreateRequest request
	) {
		FriendRequestResponse response = friendshipService.createRequest(
			principal.getAccountId(),
			request.getAddresseeAccountId()
		);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/requests/pending")
	public ResponseEntity<List<FriendRequestResponse>> listPending(
		@AuthenticationPrincipal AuthUserPrincipal principal
	) {
		return ResponseEntity.ok(friendshipService.listPendingForAccount(principal.getAccountId()));
	}

	@PostMapping("/requests/{requestId}/accept")
	public ResponseEntity<FriendRequestResponse> acceptRequest(
		@AuthenticationPrincipal AuthUserPrincipal principal,
		@PathVariable UUID requestId
	) {
		return ResponseEntity.ok(
			friendshipService.acceptRequest(requestId, principal.getAccountId(), principal.getUsername())
		);
	}

	@PostMapping("/requests/{requestId}/reject")
	public ResponseEntity<FriendRequestResponse> rejectRequest(
		@AuthenticationPrincipal AuthUserPrincipal principal,
		@PathVariable UUID requestId
	) {
		return ResponseEntity.ok(
			friendshipService.rejectRequest(requestId, principal.getAccountId(), principal.getUsername())
		);
	}
}
