package com.weetalk.chat.messages.domain;

import org.springframework.data.annotation.Transient;

public class AttachmentMeta {
	private AttachmentKind kind;
	@Transient
	private String url;
	private String filename;
	private String contentType;
	private long sizeBytes;

	public AttachmentKind getKind() {
		return kind;
	}

	public void setKind(AttachmentKind kind) {
		this.kind = kind;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}

	public void setSizeBytes(long sizeBytes) {
		this.sizeBytes = sizeBytes;
	}
}
