package com.weetalk.chat.accounts.api.dto;

import java.time.Instant;
import java.util.UUID;

public class FriendRequestResponse {
	private final UUID id;
	private final UUID requesterAccountId;
	private final String requesterDisplayName;
	private final String requesterAvatarUrl;
	private final UUID addresseeAccountId;
	private final String addresseeDisplayName;
	private final String addresseeAvatarUrl;
	private final String status;
	private final Instant requestedAt;

	public FriendRequestResponse(
		UUID id,
		UUID requesterAccountId,
		String requesterDisplayName,
		String requesterAvatarUrl,
		UUID addresseeAccountId,
		String addresseeDisplayName,
		String addresseeAvatarUrl,
		String status,
		Instant requestedAt
	) {
		this.id = id;
		this.requesterAccountId = requesterAccountId;
		this.requesterDisplayName = requesterDisplayName;
		this.requesterAvatarUrl = requesterAvatarUrl;
		this.addresseeAccountId = addresseeAccountId;
		this.addresseeDisplayName = addresseeDisplayName;
		this.addresseeAvatarUrl = addresseeAvatarUrl;
		this.status = status;
		this.requestedAt = requestedAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getRequesterAccountId() {
		return requesterAccountId;
	}

	public String getRequesterDisplayName() {
		return requesterDisplayName;
	}

	public String getRequesterAvatarUrl() {
		return requesterAvatarUrl;
	}

	public UUID getAddresseeAccountId() {
		return addresseeAccountId;
	}

	public String getAddresseeDisplayName() {
		return addresseeDisplayName;
	}

	public String getAddresseeAvatarUrl() {
		return addresseeAvatarUrl;
	}

	public String getStatus() {
		return status;
	}

	public Instant getRequestedAt() {
		return requestedAt;
	}
}
