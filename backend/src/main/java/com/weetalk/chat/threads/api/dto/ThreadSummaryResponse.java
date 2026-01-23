package com.weetalk.chat.threads.api.dto;

import com.weetalk.chat.threads.infrastructure.mongo.ThreadDoc;
import com.weetalk.chat.threads.infrastructure.mongo.ThreadMember;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ThreadSummaryResponse {
	private String id;
	private Instant createdAt;
	private Instant lastMessageAt;
	private List<UUID> memberAccountIds;

	public ThreadSummaryResponse(String id, Instant createdAt, Instant lastMessageAt, List<UUID> memberAccountIds) {
		this.id = id;
		this.createdAt = createdAt;
		this.lastMessageAt = lastMessageAt;
		this.memberAccountIds = memberAccountIds;
	}

	public String getId() {
		return id;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getLastMessageAt() {
		return lastMessageAt;
	}

	public List<UUID> getMemberAccountIds() {
		return memberAccountIds;
	}

	public static ThreadSummaryResponse from(ThreadDoc doc) {
		List<UUID> memberAccountIds = doc.getMembers()
			.stream()
			.filter(member -> member.getLeftAt() == null)
			.map(ThreadMember::getAccountId)
			.collect(Collectors.toList());

		return new ThreadSummaryResponse(doc.getId(), doc.getCreatedAt(), doc.getLastMessageAt(), memberAccountIds);
	}
}
