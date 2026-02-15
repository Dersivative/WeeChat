package com.weetalk.chat.auth.application;

import com.weetalk.chat.accounts.domain.User;
import com.weetalk.chat.accounts.infrastructure.UserRepository;
import com.weetalk.chat.auth.api.dto.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public void register(RegisterRequest request) {
		if (userRepository.findByLoginIgnoreCase(request.getLogin()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Login is already taken");
		}

		if (userRepository.findByEmailIgnoreCase(request.getEmail()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
		}

		User user = new User();
		user.setLogin(request.getLogin());
		user.setEmail(request.getEmail());
		user.setDisplayName(request.getDisplayName());
		
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

		user.setTwoFactorEnabled(false);
		user.setAvatarFileName("default.png"); 

		userRepository.save(user);
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

		if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
			return passwordEncoder.matches(rawPassword, "{bcrypt}" + storedHash);
		}

		return passwordEncoder.matches(rawPassword, "{noop}" + storedHash);
	}
}