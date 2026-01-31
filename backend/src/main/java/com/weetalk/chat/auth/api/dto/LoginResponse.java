package com.weetalk.chat.auth.api.dto;

import java.util.UUID;

public class LoginResponse {
	private UUID accountId;
	private String login;
	private boolean twoFactorEnabled;
	private String avatarUrl;

	public LoginResponse(
		UUID accountId,
		String login,
		boolean twoFactorEnabled,
		String avatarUrl
	) {
		this.accountId = accountId;
		this.login = login;
		this.twoFactorEnabled = twoFactorEnabled;
		this.avatarUrl = avatarUrl;
	}

	public UUID getAccountId() {
		return accountId;
	}

	public String getLogin() {
		return login;
	}

	public boolean isTwoFactorEnabled() {
		return twoFactorEnabled;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}
}
