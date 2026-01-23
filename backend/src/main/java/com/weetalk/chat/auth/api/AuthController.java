package com.weetalk.chat.auth.api;

import com.weetalk.chat.accounts.domain.User;
import com.weetalk.chat.accounts.infrastructure.UserRepository;
import com.weetalk.chat.auth.api.dto.LoginRequest;
import com.weetalk.chat.auth.api.dto.LoginResponse;
import com.weetalk.chat.auth.api.dto.RefreshTokenRequest;
import com.weetalk.chat.auth.api.dto.TokenResponse;
import com.weetalk.chat.auth.application.AuthService;
import com.weetalk.chat.auth.security.AuthUserPrincipal;
import com.weetalk.chat.auth.security.JwtService;
import com.weetalk.chat.media.MediaUrlResolver;
import jakarta.validation.Valid;
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

	public AuthController(
		AuthService authService,
		JwtService jwtService,
		UserRepository userRepository,
		MediaUrlResolver mediaUrlResolver
	) {
		this.authService = authService;
		this.jwtService = jwtService;
		this.userRepository = userRepository;
		this.mediaUrlResolver = mediaUrlResolver;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		User user = authService.authenticate(request.getLogin(), request.getPassword());
		String accessToken = jwtService.generateAccessToken(user);
		String refreshToken = jwtService.generateRefreshToken(user);
		LoginResponse response = new LoginResponse(
			user.getId(),
			user.getLogin(),
			user.isTwoFactorEnabled(),
			mediaUrlResolver.resolveAvatarUrl(user.getAvatarFileName()),
			accessToken,
			refreshToken
		);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		AuthUserPrincipal principal = jwtService.parseRefreshToken(request.getRefreshToken());
		User user = userRepository.findById(principal.getAccountId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
		String accessToken = jwtService.generateAccessToken(user);
		String refreshToken = jwtService.generateRefreshToken(user);
		return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken));
	}
}
