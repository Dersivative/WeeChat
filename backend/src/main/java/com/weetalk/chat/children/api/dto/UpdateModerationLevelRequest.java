package com.weetalk.chat.children.api.dto;

import com.weetalk.chat.children.domain.ModerationLevel;
import jakarta.validation.constraints.NotNull;

public class UpdateModerationLevelRequest {
	@NotNull
	private ModerationLevel moderationLevel;

	public ModerationLevel getModerationLevel() {
		return moderationLevel;
	}

	public void setModerationLevel(ModerationLevel moderationLevel) {
		this.moderationLevel = moderationLevel;
	}
}
