package com.weetalk.chat.messages.api.dto;

import java.time.Instant;
import java.util.UUID;

public class MessageItemResponse {
	private String id;
	private String threadId;
	private UUID senderAccountId;
	private String text;
	private Instant createdAt;

	public MessageItemResponse(
		String id,
		String threadId,
		UUID senderAccountId,
		String text,
		Instant createdAt
	) {
		this.id = id;
		this.threadId = threadId;
		this.senderAccountId = senderAccountId;
		this.text = text;
		this.createdAt = createdAt;
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
}
