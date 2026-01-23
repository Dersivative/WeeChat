package com.weetalk.chat.threads.infrastructure.mongo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "threads")
public class ThreadDoc {
	@Id
	private String id;

	private List<ThreadMember> members = new ArrayList<>();

	private Instant createdAt;
	private Instant lastMessageAt;
	private String customTitle;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<ThreadMember> getMembers() {
		return members;
	}

	public void setMembers(List<ThreadMember> members) {
		this.members = members;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getLastMessageAt() {
		return lastMessageAt;
	}

	public void setLastMessageAt(Instant lastMessageAt) {
		this.lastMessageAt = lastMessageAt;
	}

	public String getCustomTitle() {
		return customTitle;
	}

	public void setCustomTitle(String customTitle) {
		this.customTitle = customTitle;
	}
}
