package com.weetalk.chat.children.api;

import com.weetalk.chat.auth.security.AuthUserPrincipal;
import com.weetalk.chat.children.api.dto.ChildLoginRequest;
import com.weetalk.chat.children.api.dto.ChildLoginResponse;
import com.weetalk.chat.children.api.dto.ChildLoginTokenResponse;
import com.weetalk.chat.children.api.dto.ChildResponse;
import com.weetalk.chat.children.api.dto.CreateChildRequest;
import com.weetalk.chat.children.application.ChildAuthService;
import com.weetalk.chat.children.application.ChildManagementService;
import com.weetalk.chat.children.api.dto.UpdateModerationLevelRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/children")
public class ChildController {
	private final ChildAuthService childAuthService;
	private final ChildManagementService childManagementService;

	public ChildController(ChildAuthService childAuthService, ChildManagementService childManagementService) {
		this.childAuthService = childAuthService;
		this.childManagementService = childManagementService;
	}

	@GetMapping
	public ResponseEntity<List<ChildResponse>> listChildren(@AuthenticationPrincipal AuthUserPrincipal principal) {
		return ResponseEntity.ok(childManagementService.listChildren(principal.getAccountId()));
	}

	@PostMapping
	public ResponseEntity<ChildResponse> createChild(
		@AuthenticationPrincipal AuthUserPrincipal principal,
		@Valid @RequestBody CreateChildRequest request
	) {
		ChildResponse response = childAuthService.createChild(principal.getAccountId(), request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/{childId}/moderation-level")
	public ResponseEntity<ChildResponse> updateModerationLevel(
		@AuthenticationPrincipal AuthUserPrincipal principal,
		@PathVariable UUID childId,
		@Valid @RequestBody UpdateModerationLevelRequest request
	) {
		ChildResponse response = childManagementService.updateModerationLevel(
			principal.getAccountId(),
			childId,
			request.getModerationLevel()
		);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{childId}")
	public ResponseEntity<Void> deleteChild(
		@AuthenticationPrincipal AuthUserPrincipal principal,
		@PathVariable UUID childId
	) {
		childManagementService.deleteChild(principal.getAccountId(), childId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{childId}/login-qr-token")
	public ResponseEntity<ChildLoginTokenResponse> generateQrToken(
		@AuthenticationPrincipal AuthUserPrincipal principal,
		@PathVariable UUID childId
	) {
		return ResponseEntity.ok(childAuthService.generateQrToken(principal.getAccountId(), childId));
	}

	@PostMapping("/{childId}/login-code")
	public ResponseEntity<ChildLoginTokenResponse> generateLoginCode(
		@AuthenticationPrincipal AuthUserPrincipal principal,
		@PathVariable UUID childId
	) {
		return ResponseEntity.ok(childAuthService.generateTextCode(principal.getAccountId(), childId));
	}

	@PostMapping("/login")
	public ResponseEntity<ChildLoginResponse> login(@Valid @RequestBody ChildLoginRequest request) {
		return ResponseEntity.ok(childAuthService.loginWithToken(request.getToken()));
	}
}
