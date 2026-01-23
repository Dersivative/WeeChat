package com.weetalk.chat.messages.infrastructure.mongo;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<MessageDoc, String> {
	Page<MessageDoc> findByThreadIdOrderByCreatedAtDesc(String threadId, Pageable pageable);
	Page<MessageDoc> findByThreadIdAndCreatedAtBeforeOrderByCreatedAtDesc(String threadId, Instant before, Pageable pageable);
	MessageDoc findTopByThreadIdOrderByCreatedAtDesc(String threadId);
}
