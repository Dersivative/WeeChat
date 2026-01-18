package com.weetalk.chat.mongo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<MessageDoc, String> {
	Page<MessageDoc> findByThreadIdOrderByCreatedAtDesc(String threadId, Pageable pageable);
}
