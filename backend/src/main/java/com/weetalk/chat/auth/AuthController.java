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

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		User user = authService.authenticate(request.getLogin(), request.getPassword());
		LoginResponse response = new LoginResponse(
			user.getId(),
			user.getLogin(),
			user.isTwoFactorEnabled(),
			user.getAvatarUrl()
		);

		return ResponseEntity.ok(response);
	}
}
