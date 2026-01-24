package com.weetalk.chat.children.infrastructure;

import com.weetalk.chat.children.domain.Child;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildRepository extends JpaRepository<Child, UUID> {
	List<Child> findByLoginCodeHashIsNotNullAndLoginCodeExpiresAtAfter(Instant instant);
}
