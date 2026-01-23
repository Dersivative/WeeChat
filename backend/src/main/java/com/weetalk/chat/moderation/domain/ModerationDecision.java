package com.weetalk.chat.moderation.domain;

import java.time.Instant;
import java.util.Map;

public class ModerationDecision {
	private ModerationStatus status = ModerationStatus.PENDING;
	private String decidedByParentUsername;
	private Instant decidedAt;
	private String reason;
	private Map<String, Object> modelData;

	public ModerationDecision() {
	}

	public ModerationStatus getStatus() {
		return status;
	}

	public void setStatus(ModerationStatus status) {
		this.status = status;
	}

	public String getDecidedByParentUsername() {
		return decidedByParentUsername;
	}

	public void setDecidedByParentUsername(String decidedByParentUsername) {
		this.decidedByParentUsername = decidedByParentUsername;
	}

	public Instant getDecidedAt() {
		return decidedAt;
	}

	public void setDecidedAt(Instant decidedAt) {
		this.decidedAt = decidedAt;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Map<String, Object> getModelData() {
		return modelData;
	}

	public void setModelData(Map<String, Object> modelData) {
		this.modelData = modelData;
	}
}
