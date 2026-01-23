package com.weetalk.chat.moderation.domain;

public enum ModerationJobStatus {
	/** stany pracy modelu LLM moderującego wiadomości.*/
	QUEUED,
	RUNNING,
	COMPLETED,
	FAILED
}
