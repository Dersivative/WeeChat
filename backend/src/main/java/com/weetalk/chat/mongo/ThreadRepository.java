package com.weetalk.chat.mongo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ThreadRepository extends MongoRepository<ThreadDoc, String> {
	Page<ThreadDoc> findAllByOrderByLastMessageAtDesc(Pageable pageable);
}
