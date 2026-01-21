package com.weetalk.chat.mongo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.UUID;
import java.util.List;

public interface ThreadRepository extends MongoRepository<ThreadDoc, String> {
	Page<ThreadDoc> findAllByOrderByLastMessageAtDesc(Pageable pageable);
	Page<ThreadDoc> findByMembersAccountIdAndMembersLeftAtIsNullOrderByLastMessageAtDesc(UUID accountId, Pageable pageable);
	List<ThreadDoc> findByMembersAccountIdAndMembersLeftAtIsNullOrderByLastMessageAtDesc(UUID accountId);
}
