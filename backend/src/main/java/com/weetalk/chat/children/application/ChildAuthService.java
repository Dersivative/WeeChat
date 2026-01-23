package com.weetalk.chat.children.application;

import com.weetalk.chat.accounts.domain.User;
import com.weetalk.chat.accounts.infrastructure.UserRepository;
import com.weetalk.chat.children.api.dto.ChildLoginTokenResponse;
import com.weetalk.chat.children.api.dto.ChildResponse;
import com.weetalk.chat.children.api.dto.CreateChildRequest;
import com.weetalk.chat.children.domain.Child;
import com.weetalk.chat.children.domain.ChildLoginCodeType;
import com.weetalk.chat.children.infrastructure.ChildRepository;
import com.weetalk.chat.media.MediaUrlResolver;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChildAuthService {
	private static final Duration QR_CODE_TTL = Duration.ofSeconds(60);
	private static final Duration TEXT_CODE_TTL = Duration.ofSeconds(30);
	private static final int TEXT_CODE_DIGITS = 6;
	private static final int QR_TOKEN_BYTES = 32;

	private final UserRepository userRepository;
	private final ChildRepository childRepository;
	private final PasswordEncoder passwordEncoder;
	private final MediaUrlResolver mediaUrlResolver;
	private final SecureRandom secureRandom = new SecureRandom();
	private final Base64.Encoder base64UrlEncoder = Base64.getUrlEncoder().withoutPadding();

	public ChildAuthService(
		UserRepository userRepository,
		ChildRepository childRepository,
		PasswordEncoder passwordEncoder,
		MediaUrlResolver mediaUrlResolver
	) {
		this.userRepository = userRepository;
		this.childRepository = childRepository;
		this.passwordEncoder = passwordEncoder;
		this.mediaUrlResolver = mediaUrlResolver;
	}

	@Transactional
	public ChildResponse createChild(UUID parentId, CreateChildRequest request) {
		User parent = loadParent(parentId);
		Child child = new Child();
		child.setDisplayName(request.getDisplayName());
		child.setAvatarFileName(request.getAvatarFileName());
		child.getParents().add(parent);
		parent.getChildren().add(child);
		childRepository.save(child);
		userRepository.save(parent);

		return new ChildResponse(
			child.getId(),
			child.getDisplayName(),
			mediaUrlResolver.resolveAvatarUrl(child.getAvatarFileName())
		);
	}

	@Transactional
	public ChildLoginTokenResponse generateQrToken(UUID parentId, UUID childId) {
		User parent = loadParent(parentId);
		Child child = loadChildForParent(parent, childId);
		String token = randomQrToken();
		Instant expiresAt = Instant.now().plus(QR_CODE_TTL);
		storeLoginToken(child, token, ChildLoginCodeType.QR_CODE, expiresAt);
		return new ChildLoginTokenResponse(token, expiresAt);
	}

	@Transactional
	public ChildLoginTokenResponse generateTextCode(UUID parentId, UUID childId) {
		User parent = loadParent(parentId);
		Child child = loadChildForParent(parent, childId);
		String token = randomTextCode();
		Instant expiresAt = Instant.now().plus(TEXT_CODE_TTL);
		storeLoginToken(child, token, ChildLoginCodeType.TEXT_CODE, expiresAt);
		return new ChildLoginTokenResponse(token, expiresAt);
	}

	private User loadParent(UUID parentId) {
		return userRepository.findById(parentId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Parent account required"));
	}

	private Child loadChildForParent(User parent, UUID childId) {
		Child child = childRepository.findById(childId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
		boolean linked = parent.getChildren()
			.stream()
			.anyMatch(parentChild -> childId.equals(parentChild.getId()));
		if (!linked) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Parent not linked to child");
		}
		return child;
	}

	private void storeLoginToken(Child child, String token, ChildLoginCodeType type, Instant expiresAt) {
		child.setLoginCodeHash(passwordEncoder.encode(token));
		child.setLoginCodeType(type);
		child.setLoginCodeExpiresAt(expiresAt);
		childRepository.save(child);
	}

	private String randomQrToken() {
		byte[] bytes = new byte[QR_TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return base64UrlEncoder.encodeToString(bytes);
	}

	private String randomTextCode() {
		int bound = (int) Math.pow(10, TEXT_CODE_DIGITS);
		int code = secureRandom.nextInt(bound - (bound / 10)) + (bound / 10);
		return Integer.toString(code);
	}
}
