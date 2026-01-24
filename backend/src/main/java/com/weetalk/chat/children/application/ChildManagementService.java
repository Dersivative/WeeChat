package com.weetalk.chat.children.application;

import com.weetalk.chat.accounts.domain.User;
import com.weetalk.chat.accounts.infrastructure.UserRepository;
import com.weetalk.chat.children.api.dto.ChildResponse;
import com.weetalk.chat.children.domain.Child;
import com.weetalk.chat.children.domain.ModerationLevel;
import com.weetalk.chat.children.infrastructure.ChildRepository;
import com.weetalk.chat.media.MediaUrlResolver;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChildManagementService {
	private final UserRepository userRepository;
	private final ChildRepository childRepository;
	private final MediaUrlResolver mediaUrlResolver;

	public ChildManagementService(
		UserRepository userRepository,
		ChildRepository childRepository,
		MediaUrlResolver mediaUrlResolver
	) {
		this.userRepository = userRepository;
		this.childRepository = childRepository;
		this.mediaUrlResolver = mediaUrlResolver;
	}

	@Transactional(readOnly = true)
	public List<ChildResponse> listChildren(UUID parentId) {
		User parent = loadParent(parentId);
		return parent.getChildren()
			.stream()
			.map(child -> toResponse(child))
			.toList();
	}

	@Transactional
	public ChildResponse updateModerationLevel(UUID parentId, UUID childId, ModerationLevel moderationLevel) {
		User parent = loadParent(parentId);
		Child child = loadChildForParent(parent, childId);
		child.setModerationLevel(moderationLevel == null ? ModerationLevel.MANUAL : moderationLevel);
		childRepository.save(child);
		return toResponse(child);
	}

	@Transactional
	public void deleteChild(UUID parentId, UUID childId) {
		User parent = loadParent(parentId);
		Child child = loadChildForParent(parent, childId);
		parent.getChildren().remove(child);
		child.getParents().remove(parent);
		userRepository.save(parent);
		childRepository.delete(child);
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

	private ChildResponse toResponse(Child child) {
		return new ChildResponse(
			child.getId(),
			child.getDisplayName(),
			mediaUrlResolver.resolveAvatarUrl(child.getAvatarFileName()),
			child.getModerationLevel()
		);
	}
}
