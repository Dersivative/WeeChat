package com.weetalk.chat.messages.api.dto;

import java.util.List;

public class ModerationQueueResponse {
	private List<ModerationThreadResponse> threads;

	public ModerationQueueResponse(List<ModerationThreadResponse> threads) {
		this.threads = threads;
	}

	public List<ModerationThreadResponse> getThreads() {
		return threads;
	}
}
