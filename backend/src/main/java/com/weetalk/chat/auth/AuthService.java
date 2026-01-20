package com.weetalk.chat.auth;

import com.weetalk.chat.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public User authenticate(String login, String password) {
		User user = userRepository.findByLoginIgnoreCase(login)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

		if (!matchesPassword(password, user.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		}

		return user;
	}

	private boolean matchesPassword(String rawPassword, String storedHash) {
		if (storedHash == null || rawPassword == null) {
			return false;
		}

		if (storedHash.startsWith("{")) {
			return passwordEncoder.matches(rawPassword, storedHash);
		}

		return passwordEncoder.matches(rawPassword, "{noop}" + storedHash);
	}
}
