package com.weetalk.chat.messages.infrastructure.mongo;

import com.weetalk.chat.messages.domain.AttachmentMeta;
import com.weetalk.chat.messages.domain.MessageDeliveryStatus;
import com.weetalk.chat.moderation.domain.ModerationDecision;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "messages")
public class MessageDoc {
	@Id
	private String id;

	private String threadId;
	private UUID senderAccountId;
	private String text;
	private List<AttachmentMeta> attachments = new ArrayList<>();
	private List<MessageDeliveryStatus> deliveryStatuses = new ArrayList<>();
	private ModerationDecision moderationDecision;
	private Instant deletedAt; // wiadomość nie będzie usuwana z bazy, natomiast użytkownik nie będzie w stanie odczytać jej treści (endpoint nie zwroci tresci)
	private UUID deletedByAccountId;
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

	public UUID getSenderAccountId() {
		return senderAccountId;
	}

	public void setSenderAccountId(UUID senderAccountId) {
		this.senderAccountId = senderAccountId;
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

	public List<MessageDeliveryStatus> getDeliveryStatuses() {
		return deliveryStatuses;
	}

	public void setDeliveryStatuses(List<MessageDeliveryStatus> deliveryStatuses) {
		this.deliveryStatuses = deliveryStatuses;
	}

	public ModerationDecision getModerationDecision() {
		return moderationDecision;
	}

	public void setModerationDecision(ModerationDecision moderationDecision) {
		this.moderationDecision = moderationDecision;
	}

	public Instant getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}

	public UUID getDeletedByAccountId() {
		return deletedByAccountId;
	}

	public void setDeletedByAccountId(UUID deletedByAccountId) {
		this.deletedByAccountId = deletedByAccountId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
