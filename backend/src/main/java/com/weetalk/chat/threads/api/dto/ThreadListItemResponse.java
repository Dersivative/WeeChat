package com.weetalk.chat.threads.api.dto;

import java.time.Instant;
import java.util.List;

public class ThreadListItemResponse {
	private String threadId;
	private String title;
	private String lastMessageText;
	private Instant lastMessageAt;
	private boolean unread;
	private List<String> avatarUrls;

	public ThreadListItemResponse(
		String threadId,
		String title,
		String lastMessageText,
		Instant lastMessageAt,
		boolean unread,
		List<String> avatarUrls
	) {
		this.threadId = threadId;
		this.title = title;
		this.lastMessageText = lastMessageText;
		this.lastMessageAt = lastMessageAt;
		this.unread = unread;
		this.avatarUrls = avatarUrls;
	}

	public String getThreadId() {
		return threadId;
	}

	public String getTitle() {
		return title;
	}

	public String getLastMessageText() {
		return lastMessageText;
	}

	public Instant getLastMessageAt() {
		return lastMessageAt;
	}

	public boolean isUnread() {
		return unread;
	}

	public List<String> getAvatarUrls() {
		return avatarUrls;
	}
}
