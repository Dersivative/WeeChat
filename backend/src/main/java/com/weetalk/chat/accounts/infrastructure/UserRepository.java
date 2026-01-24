package com.weetalk.chat.accounts.infrastructure;

import com.weetalk.chat.accounts.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByLoginIgnoreCase(String login);
	Optional<User> findByEmailIgnoreCase(String email);

	@Query("select count(u) > 0 from User u join u.children c where u.id = :parentId and c.id = :childId")
	boolean isParentOfChild(@Param("parentId") UUID parentId, @Param("childId") UUID childId);
}
