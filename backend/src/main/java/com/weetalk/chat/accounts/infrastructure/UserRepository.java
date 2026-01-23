package com.weetalk.chat.accounts.infrastructure;

import com.weetalk.chat.accounts.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByLoginIgnoreCase(String login);
}
