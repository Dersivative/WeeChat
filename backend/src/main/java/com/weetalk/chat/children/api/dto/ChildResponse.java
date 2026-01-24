package com.weetalk.chat.children.api.dto;

import java.util.UUID;
import com.weetalk.chat.children.domain.ModerationLevel;

public class ChildResponse {
	private final UUID id;
	private final String displayName;
	private final String avatarUrl;
	private final ModerationLevel moderationLevel;

	public ChildResponse(UUID id, String displayName, String avatarUrl, ModerationLevel moderationLevel) {
		this.id = id;
		this.displayName = displayName;
		this.avatarUrl = avatarUrl;
		this.moderationLevel = moderationLevel;
	}

	public UUID getId() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public ModerationLevel getModerationLevel() {
		return moderationLevel;
	}
}
