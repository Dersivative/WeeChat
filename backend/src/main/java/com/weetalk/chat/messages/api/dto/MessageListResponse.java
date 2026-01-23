package com.weetalk.chat.messages.api.dto;

import java.time.Instant;
import java.util.List;

public class MessageListResponse {
	private List<MessageItemResponse> messages;
	private boolean hasMore;
	private Instant nextBefore;

	public MessageListResponse(List<MessageItemResponse> messages, boolean hasMore, Instant nextBefore) {
		this.messages = messages;
		this.hasMore = hasMore;
		this.nextBefore = nextBefore;
	}

	public List<MessageItemResponse> getMessages() {
		return messages;
	}

	public boolean isHasMore() {
		return hasMore;
	}

	public Instant getNextBefore() {
		return nextBefore;
	}
}
