package com.weetalk.chat.auth.api;

import com.weetalk.chat.accounts.domain.User;
import com.weetalk.chat.accounts.infrastructure.UserRepository;
import com.weetalk.chat.auth.api.dto.LoginRequest;
import com.weetalk.chat.auth.api.dto.LoginResponse;
import com.weetalk.chat.auth.api.dto.RefreshTokenRequest;
import com.weetalk.chat.auth.api.dto.RegisterRequest;
import com.weetalk.chat.auth.application.AuthService;
import com.weetalk.chat.auth.security.AuthCookieService;
import com.weetalk.chat.auth.security.AuthUserPrincipal;
import com.weetalk.chat.auth.security.JwtService;
import com.weetalk.chat.media.MediaUrlResolver;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final AuthService authService;
	private final JwtService jwtService;
	private final UserRepository userRepository;
	private final MediaUrlResolver mediaUrlResolver;
	private final AuthCookieService authCookieService;
	@Value("${security.jwt.access-expiration-seconds:300}")
	private long accessExpirationSeconds;
	@Value("${security.jwt.refresh-expiration-seconds:2592000}")
	private long refreshExpirationSeconds;

	public AuthController(
		AuthService authService,
		JwtService jwtService,
		UserRepository userRepository,
		MediaUrlResolver mediaUrlResolver,
		AuthCookieService authCookieService
	) {
		this.authService = authService;
		this.jwtService = jwtService;
		this.userRepository = userRepository;
		this.mediaUrlResolver = mediaUrlResolver;
		this.authCookieService = authCookieService;
	}
	
	@PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		User user = authService.authenticate(request.getLogin(), request.getPassword());
		String accessToken = jwtService.generateAccessToken(user);
		String refreshToken = jwtService.generateRefreshToken(user);
		var accessCookie = authCookieService.buildAccessCookie(accessToken, accessExpirationSeconds);
		var refreshCookie = authCookieService.buildRefreshCookie(refreshToken, refreshExpirationSeconds);
		LoginResponse response = new LoginResponse(
			user.getId(),
			user.getLogin(),
			user.isTwoFactorEnabled(),
			mediaUrlResolver.resolveAvatarUrl(user.getAvatarFileName())
		);

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, accessCookie.toString())
			.header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
			.body(response);
	}

	@PostMapping("/refresh")
	public ResponseEntity<LoginResponse> refresh(
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

		AuthUserPrincipal principal = jwtService.parseRefreshToken(refreshToken);
		User user = userRepository.findById(principal.getAccountId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
		String accessToken = jwtService.generateAccessToken(user);
		String refreshTokenValue = jwtService.generateRefreshToken(user);
		var accessCookie = authCookieService.buildAccessCookie(accessToken, accessExpirationSeconds);
		var refreshCookie = authCookieService.buildRefreshCookie(refreshTokenValue, refreshExpirationSeconds);
		LoginResponse response = new LoginResponse(
			user.getId(),
			user.getLogin(),
			user.isTwoFactorEnabled(),
			mediaUrlResolver.resolveAvatarUrl(user.getAvatarFileName())
		);
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, accessCookie.toString())
			.header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
			.body(response);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		var accessCookie = authCookieService.clearAccessCookie();
		var refreshCookie = authCookieService.clearRefreshCookie();
		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, accessCookie.toString())
			.header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
			.build();
	}
}
