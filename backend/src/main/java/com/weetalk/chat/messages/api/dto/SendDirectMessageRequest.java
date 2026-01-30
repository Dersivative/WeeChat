package com.weetalk.chat.messages.api.dto;

import java.util.UUID;

public class SendDirectMessageRequest {
	private UUID recipientAccountId;
	private String text;

	public UUID getRecipientAccountId() {
		return recipientAccountId;
	}

	public void setRecipientAccountId(UUID recipientAccountId) {
		this.recipientAccountId = recipientAccountId;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
