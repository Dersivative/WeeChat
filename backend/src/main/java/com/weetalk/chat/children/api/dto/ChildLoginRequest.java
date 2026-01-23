package com.weetalk.chat.children.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ChildLoginRequest {
	@NotNull
	private UUID childId;

	@NotBlank
	private String token;

	public UUID getChildId() {
		return childId;
	}

	public void setChildId(UUID childId) {
		this.childId = childId;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
