package com.weetalk.chat.mongo;

import com.weetalk.chat.domain.AttachmentMeta;
import com.weetalk.chat.domain.ModerationDecision;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "messages")
public class MessageDoc {
	@Id
	private String id;

	private String threadId;
	private ThreadMember sender;
	private String text;
	private List<AttachmentMeta> attachments = new ArrayList<>();
	private ModerationDecision moderationDecision;
	private Instant createdAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getThreadId() {
		return threadId;
	}

	public void setThreadId(String threadId) {
		this.threadId = threadId;
	}

	public ThreadMember getSender() {
		return sender;
	}

	public void setSender(ThreadMember sender) {
		this.sender = sender;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public List<AttachmentMeta> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<AttachmentMeta> attachments) {
		this.attachments = attachments;
	}

	public ModerationDecision getModerationDecision() {
		return moderationDecision;
	}

	public void setModerationDecision(ModerationDecision moderationDecision) {
		this.moderationDecision = moderationDecision;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
