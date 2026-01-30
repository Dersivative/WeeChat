package com.weetalk.chat.threads.infrastructure.mongo;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ThreadRepository extends MongoRepository<ThreadDoc, String> {
	Page<ThreadDoc> findAllByOrderByLastMessageAtDesc(Pageable pageable);
	Page<ThreadDoc> findByMembersAccountIdAndMembersLeftAtIsNullOrderByLastMessageAtDesc(UUID accountId, Pageable pageable);
	List<ThreadDoc> findByMembersAccountIdAndMembersLeftAtIsNullOrderByLastMessageAtDesc(UUID accountId);

	@Query("{ 'members': { $all: [ { $elemMatch: { accountId: ?0, leftAt: null } }, { $elemMatch: { accountId: ?1, leftAt: null } } ] } }")
	List<ThreadDoc> findDirectThreads(UUID firstAccountId, UUID secondAccountId);
}
