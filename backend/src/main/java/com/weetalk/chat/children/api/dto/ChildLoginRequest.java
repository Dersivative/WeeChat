package com.weetalk.chat.children.api.dto;

import jakarta.validation.constraints.NotBlank;
public class ChildLoginRequest {
	@NotBlank
	private String token;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
