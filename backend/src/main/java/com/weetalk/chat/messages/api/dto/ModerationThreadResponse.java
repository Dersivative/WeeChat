package com.weetalk.chat.messages.api.dto;

import java.time.Instant;
import java.util.List;

public class ModerationThreadResponse {
	private String threadId;
	private String title;
	private Instant lastMessageAt;
	private List<String> avatarUrls;
	private List<ModerationMessageResponse> messages;

	public ModerationThreadResponse(
		String threadId,
		String title,
		Instant lastMessageAt,
		List<String> avatarUrls,
		List<ModerationMessageResponse> messages
	) {
		this.threadId = threadId;
		this.title = title;
		this.lastMessageAt = lastMessageAt;
		this.avatarUrls = avatarUrls;
		this.messages = messages;
	}

	public String getThreadId() {
		return threadId;
	}

	public String getTitle() {
		return title;
	}

	public Instant getLastMessageAt() {
		return lastMessageAt;
	}

	public List<String> getAvatarUrls() {
		return avatarUrls;
	}

	public List<ModerationMessageResponse> getMessages() {
		return messages;
	}
}
