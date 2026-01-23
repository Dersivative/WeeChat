package com.weetalk.chat.children.api.dto;

import java.util.UUID;

public class ChildResponse {
	private final UUID id;
	private final String displayName;
	private final String avatarUrl;

	public ChildResponse(UUID id, String displayName, String avatarUrl) {
		this.id = id;
		this.displayName = displayName;
		this.avatarUrl = avatarUrl;
	}

	public UUID getId() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}
}
