package com.weetalk.chat.children.infrastructure;

import com.weetalk.chat.children.domain.Child;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildRepository extends JpaRepository<Child, UUID> {
}
