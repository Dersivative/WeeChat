package com.weetalk.chat.children.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.weetalk.chat.children.domain.ModerationLevel;

public class CreateChildRequest {
	@NotBlank
	@Size(max = 100)
	private String displayName;

	@Size(max = 500)
	private String avatarFileName;

	private ModerationLevel moderationLevel;

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getAvatarFileName() {
		return avatarFileName;
	}

	public void setAvatarFileName(String avatarFileName) {
		this.avatarFileName = avatarFileName;
	}

	public ModerationLevel getModerationLevel() {
		return moderationLevel;
	}

	public void setModerationLevel(ModerationLevel moderationLevel) {
		this.moderationLevel = moderationLevel;
	}
}
