package com.weetalk.chat.auth.security;

import com.weetalk.chat.accounts.domain.User;
import com.weetalk.chat.accounts.infrastructure.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	private final UserRepository userRepository;

	public UserDetailsServiceImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByLoginIgnoreCase(username)
			.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		String passwordHash = user.getPasswordHash();
		if (passwordHash == null) {
			passwordHash = "";
		} else if (!passwordHash.startsWith("{")) {
			if (passwordHash.startsWith("$2a$") || passwordHash.startsWith("$2b$") || passwordHash.startsWith("$2y$")) {
				passwordHash = "{bcrypt}" + passwordHash;
			} else {
				passwordHash = "{noop}" + passwordHash;
			}
		}

		return new AuthUserPrincipal(user.getId(), user.getLogin(), passwordHash);
	}
}
