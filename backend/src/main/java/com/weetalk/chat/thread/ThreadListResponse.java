package com.weetalk.chat.thread;

import java.util.List;

public class ThreadListResponse {
	private List<ThreadListItemResponse> threads;

	public ThreadListResponse(List<ThreadListItemResponse> threads) {
		this.threads = threads;
	}

	public List<ThreadListItemResponse> getThreads() {
		return threads;
	}
}
