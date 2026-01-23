package com.weetalk.chat.threads.infrastructure.mongo;

import java.time.Instant;
import java.util.UUID;

public class ThreadMember {
	private UUID accountId;
	private Instant joinedAt;
	private Instant leftAt;
	private Instant lastReadAt;

	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(UUID accountId) {
		this.accountId = accountId;
	}

	public Instant getJoinedAt() {
		return joinedAt;
	}

	public void setJoinedAt(Instant joinedAt) {
		this.joinedAt = joinedAt;
	}

	public Instant getLeftAt() {
		return leftAt;
	}

	public void setLeftAt(Instant leftAt) {
		this.leftAt = leftAt;
	}

	public Instant getLastReadAt() {
		return lastReadAt;
	}

	public void setLastReadAt(Instant lastReadAt) {
		this.lastReadAt = lastReadAt;
	}
}
