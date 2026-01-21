package com.weetalk.chat.auth;

import com.weetalk.chat.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final AuthService authService;
	private final JwtService jwtService;

	public AuthController(AuthService authService, JwtService jwtService) {
		this.authService = authService;
		this.jwtService = jwtService;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		User user = authService.authenticate(request.getLogin(), request.getPassword());
		String token = jwtService.generateToken(user);
		LoginResponse response = new LoginResponse(
			user.getId(),
			user.getLogin(),
			user.isTwoFactorEnabled(),
			user.getAvatarUrl(),
			token
		);

		return ResponseEntity.ok(response);
	}
}
