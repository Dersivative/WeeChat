package com.weetalk.chat.auth;

import com.weetalk.chat.domain.User;
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

	public AuthController(AuthService authService, JwtService jwtService, UserRepository userRepository) {
		this.authService = authService;
		this.jwtService = jwtService;
		this.userRepository = userRepository;
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
			user.getAvatarUrl(),
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
