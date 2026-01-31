package com.weetalk.chat.children.api.dto;

import java.util.UUID;

public class ChildLoginResponse {
	private final UUID accountId;
	private final String displayName;
	private final String avatarUrl;

	public ChildLoginResponse(UUID accountId, String displayName, String avatarUrl) {
		this.accountId = accountId;
		this.displayName = displayName;
		this.avatarUrl = avatarUrl;
	}

	public UUID getAccountId() {
		return accountId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}
}
