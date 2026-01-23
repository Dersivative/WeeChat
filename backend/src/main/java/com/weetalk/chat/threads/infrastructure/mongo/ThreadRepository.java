package com.weetalk.chat.threads.infrastructure.mongo;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ThreadRepository extends MongoRepository<ThreadDoc, String> {
	Page<ThreadDoc> findAllByOrderByLastMessageAtDesc(Pageable pageable);
	Page<ThreadDoc> findByMembersAccountIdAndMembersLeftAtIsNullOrderByLastMessageAtDesc(UUID accountId, Pageable pageable);
	List<ThreadDoc> findByMembersAccountIdAndMembersLeftAtIsNullOrderByLastMessageAtDesc(UUID accountId);
}
