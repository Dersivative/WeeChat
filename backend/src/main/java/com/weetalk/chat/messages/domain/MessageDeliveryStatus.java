package com.weetalk.chat.messages.domain;

import java.time.Instant;
import java.util.UUID;

public class MessageDeliveryStatus {
	private UUID accountId; // ID konta odbiorcy wiadomości
	private MessageDeliveryState state = MessageDeliveryState.SENT;
	private Instant updatedAt;
	
	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(UUID accountId) {
		this.accountId = accountId;
	}

	public MessageDeliveryState getState() {
		return state;
	}

	public void setState(MessageDeliveryState state) {
		this.state = state;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
