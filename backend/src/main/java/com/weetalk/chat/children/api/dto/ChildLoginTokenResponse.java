package com.weetalk.chat.children.api.dto;

import java.time.Instant;

public class ChildLoginTokenResponse {
	private final String token;
	private final Instant expiresAt;

	public ChildLoginTokenResponse(String token, Instant expiresAt) {
		this.token = token;
		this.expiresAt = expiresAt;
	}

	public String getToken() {
		return token;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}
}
