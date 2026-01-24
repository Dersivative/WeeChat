package com.weetalk.chat.accounts.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class FriendRequestCreateRequest {
	@NotNull
	private UUID addresseeAccountId;

	public UUID getAddresseeAccountId() {
		return addresseeAccountId;
	}

	public void setAddresseeAccountId(UUID addresseeAccountId) {
		this.addresseeAccountId = addresseeAccountId;
	}
}
