package com.weetalk.chat.mongo;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ModerationJobRepository extends MongoRepository<ModerationJobDoc, String> {
	Optional<ModerationJobDoc> findFirstByMessageIdOrderByCreatedAtDesc(String messageId);
}
