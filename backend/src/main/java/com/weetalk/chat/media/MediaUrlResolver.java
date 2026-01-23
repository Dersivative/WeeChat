package com.weetalk.chat.media;

import com.weetalk.chat.messages.domain.AttachmentKind;
import org.springframework.stereotype.Component;

@Component
public class MediaUrlResolver {
	private final MediaBaseUrlProperties properties;

	public MediaUrlResolver(MediaBaseUrlProperties properties) {
		this.properties = properties;
	}

	public String resolveAvatarUrl(String fileName) {
		return join(properties.getAvatar(), fileName);
	}

	public String resolveAttachmentUrl(AttachmentKind kind, String fileName) {
		if (kind == null) {
			return join(properties.getFile(), fileName);
		}

		return switch (kind) {
			case IMAGE -> join(properties.getImage(), fileName);
			case FILE -> join(properties.getFile(), fileName);
			case AUDIO -> join(properties.getAudio(), fileName);
			case VIDEO -> join(properties.getVideo(), fileName);
		};
	}

	private String join(String baseUrl, String fileName) {
		if (fileName == null || fileName.isBlank()) {
			return null;
		}
		if (baseUrl == null || baseUrl.isBlank()) {
			return fileName;
		}

		String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		String normalizedFile = fileName.startsWith("/") ? fileName.substring(1) : fileName;
		return normalizedBase + "/" + normalizedFile;
	}
}
