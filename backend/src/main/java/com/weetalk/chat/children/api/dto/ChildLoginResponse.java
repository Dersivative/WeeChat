package com.weetalk.chat.children.api.dto;

import java.util.UUID;

public class ChildLoginResponse {
	private final UUID accountId;
	private final String displayName;
	private final String avatarUrl;
	private final String accessToken;

	public ChildLoginResponse(UUID accountId, String displayName, String avatarUrl, String accessToken) {
		this.accountId = accountId;
		this.displayName = displayName;
		this.avatarUrl = avatarUrl;
		this.accessToken = accessToken;
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

	public String getAccessToken() {
		return accessToken;
	}
}
