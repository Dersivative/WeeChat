package com.weetalk.chat.children.api;

import com.weetalk.chat.auth.security.AuthUserPrincipal;
import com.weetalk.chat.auth.security.AuthCookieService;
import com.weetalk.chat.auth.api.dto.RefreshTokenRequest;
import com.weetalk.chat.children.api.dto.ChildLoginRequest;
import com.weetalk.chat.children.api.dto.ChildLoginResponse;
import com.weetalk.chat.children.api.dto.ChildLoginTokenResponse;
import com.weetalk.chat.children.api.dto.ChildResponse;
import com.weetalk.chat.children.api.dto.CreateChildRequest;
import com.weetalk.chat.children.api.dto.LinkChildRequest;
import com.weetalk.chat.children.application.ChildLoginResult;
import com.weetalk.chat.children.application.ChildAuthService;
import com.weetalk.chat.children.application.ChildManagementService;
import com.weetalk.chat.children.api.dto.UpdateModerationLevelRequest;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/children")
public class ChildController {
	private final ChildAuthService childAuthService;
	private final ChildManagementService childManagementService;
	private final AuthCookieService authCookieService;
	@Value("${security.jwt.access-expiration-seconds:300}")
	private long accessExpirationSeconds;
	@Value("${security.jwt.refresh-expiration-seconds:2592000}")
	private long refreshExpirationSeconds;

	public ChildController(
		ChildAuthService childAuthService,
		ChildManagementService childManagementService,
		AuthCookieService authCookieService
	) {
		this.childAuthService = childAuthService;
		this.childManagementService = childManagementService;
		this.authCookieService = authCookieService;
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
		ChildLoginResult result = childAuthService.loginWithToken(request.getToken());
		var accessCookie = authCookieService.buildAccessCookie(result.accessToken(), accessExpirationSeconds);
		var refreshCookie = authCookieService.buildRefreshCookie(result.refreshToken(), refreshExpirationSeconds);
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, accessCookie.toString())
			.header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
			.body(result.response());
	}

	@PostMapping("/refresh")
	public ResponseEntity<ChildLoginResponse> refresh(
		@RequestBody(required = false) RefreshTokenRequest request,
		HttpServletRequest httpRequest
	) {
		String refreshToken = request != null ? request.getRefreshToken() : null;
		if (refreshToken == null || refreshToken.isBlank()) {
			refreshToken = authCookieService.readCookie(httpRequest, authCookieService.getRefreshCookieName());
		}
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
		}
		ChildLoginResult result = childAuthService.refreshWithToken(refreshToken);
		var accessCookie = authCookieService.buildAccessCookie(result.accessToken(), accessExpirationSeconds);
		var refreshCookie = authCookieService.buildRefreshCookie(result.refreshToken(), refreshExpirationSeconds);
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, accessCookie.toString())
			.header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
			.body(result.response());
	}
	@PostMapping("/link")
	public ChildResponse linkChild(
		@AuthenticationPrincipal AuthUserPrincipal principal, 
		@RequestBody LinkChildRequest request
	) {
		return childAuthService.linkChildToParent(principal.getAccountId(), request.getToken());
	}
}
