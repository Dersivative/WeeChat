package com.weetalk.chat.auth;

import java.util.UUID;

public class LoginResponse {
	private UUID accountId;
	private String login;
	private boolean twoFactorEnabled;
	private String avatarUrl;
	private String accessToken;
	private String refreshToken;

	public LoginResponse(
		UUID accountId,
		String login,
		boolean twoFactorEnabled,
		String avatarUrl,
		String accessToken,
		String refreshToken
	) {
		this.accountId = accountId;
		this.login = login;
		this.twoFactorEnabled = twoFactorEnabled;
		this.avatarUrl = avatarUrl;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
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

	public String getAccessToken() {
		return accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}
}
