package com.weetalk.chat.accounts.api.dto;

import java.util.UUID;

public class FriendSearchResultResponse {
	private final UUID accountId;
	private final String displayName;
	private final String avatarUrl;
	private final String accountType;
	private final String friendshipStatus;

	public FriendSearchResultResponse(
		UUID accountId,
		String displayName,
		String avatarUrl,
		String accountType,
		String friendshipStatus
	) {
		this.accountId = accountId;
		this.displayName = displayName;
		this.avatarUrl = avatarUrl;
		this.accountType = accountType;
		this.friendshipStatus = friendshipStatus;
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

	public String getAccountType() {
		return accountType;
	}

	public String getFriendshipStatus() {
		return friendshipStatus;
	}
}
