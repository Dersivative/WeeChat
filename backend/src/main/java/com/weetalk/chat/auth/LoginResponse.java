package com.weetalk.chat.auth;

import java.util.UUID;

public class LoginResponse {
	private UUID accountId;
	private String login;
	private boolean twoFactorEnabled;
	private String avatarUrl;
	private String token;

	public LoginResponse(UUID accountId, String login, boolean twoFactorEnabled, String avatarUrl, String token) {
		this.accountId = accountId;
		this.login = login;
		this.twoFactorEnabled = twoFactorEnabled;
		this.avatarUrl = avatarUrl;
		this.token = token;
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

	public String getToken() {
		return token;
	}
}
