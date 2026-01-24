package com.weetalk.chat.accounts.api.dto;

import java.util.UUID;

public class FriendListItemResponse {
	private final UUID accountId;
	private final String displayName;
	private final String avatarUrl;

	public FriendListItemResponse(UUID accountId, String displayName, String avatarUrl) {
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
