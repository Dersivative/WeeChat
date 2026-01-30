package com.weetalk.chat.messages.api.dto;

import com.weetalk.chat.moderation.domain.ModerationStatus;
import java.time.Instant;
import java.util.UUID;

public class ModerationMessageResponse {
	private String id;
	private String threadId;
	private UUID senderAccountId;
	private String text;
	private Instant createdAt;
	private ModerationStatus status;
	private ModerationStatus suggestedStatus;
	private Double score;
	private String label;

	public ModerationMessageResponse(
		String id,
		String threadId,
		UUID senderAccountId,
		String text,
		Instant createdAt,
		ModerationStatus status,
		ModerationStatus suggestedStatus,
		Double score,
		String label
	) {
		this.id = id;
		this.threadId = threadId;
		this.senderAccountId = senderAccountId;
		this.text = text;
		this.createdAt = createdAt;
		this.status = status;
		this.suggestedStatus = suggestedStatus;
		this.score = score;
		this.label = label;
	}

	public String getId() {
		return id;
	}

	public String getThreadId() {
		return threadId;
	}

	public UUID getSenderAccountId() {
		return senderAccountId;
	}

	public String getText() {
		return text;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public ModerationStatus getStatus() {
		return status;
	}

	public ModerationStatus getSuggestedStatus() {
		return suggestedStatus;
	}

	public Double getScore() {
		return score;
	}

	public String getLabel() {
		return label;
	}
}
