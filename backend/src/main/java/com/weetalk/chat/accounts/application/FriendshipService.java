package com.weetalk.chat.accounts.application;

import com.weetalk.chat.accounts.api.dto.FriendListItemResponse;
import com.weetalk.chat.accounts.api.dto.FriendRequestResponse;
import com.weetalk.chat.accounts.api.dto.FriendSearchResponse;
import com.weetalk.chat.accounts.api.dto.FriendSearchResultResponse;
import com.weetalk.chat.accounts.domain.Account;
import com.weetalk.chat.accounts.domain.AccountFriendship;
import com.weetalk.chat.accounts.domain.FriendshipStatus;
import com.weetalk.chat.accounts.domain.User;
import com.weetalk.chat.accounts.infrastructure.AccountFriendshipRepository;
import com.weetalk.chat.accounts.infrastructure.AccountRepository;
import com.weetalk.chat.accounts.infrastructure.UserRepository;
import com.weetalk.chat.children.domain.Child;
import com.weetalk.chat.children.infrastructure.ChildRepository;
import com.weetalk.chat.media.MediaUrlResolver;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FriendshipService {
	private final AccountFriendshipRepository friendshipRepository;
	private final AccountRepository accountRepository;
	private final UserRepository userRepository;
	private final ChildRepository childRepository;
	private final MediaUrlResolver mediaUrlResolver;

	public FriendshipService(
		AccountFriendshipRepository friendshipRepository,
		AccountRepository accountRepository,
		UserRepository userRepository,
		ChildRepository childRepository,
		MediaUrlResolver mediaUrlResolver
	) {
		this.friendshipRepository = friendshipRepository;
		this.accountRepository = accountRepository;
		this.userRepository = userRepository;
		this.childRepository = childRepository;
		this.mediaUrlResolver = mediaUrlResolver;
	}

	@Transactional(readOnly = true)
	public List<FriendListItemResponse> listFriends(UUID accountId) {
		return friendshipRepository
			.findByStatusAndAccountId(FriendshipStatus.ACCEPTED, accountId)
			.stream()
			.map((friendship) -> resolveFriendAccount(friendship, accountId))
			.map(this::toFriendListItem)
			.sorted(Comparator.comparing((FriendListItemResponse item) -> item.getDisplayName().toLowerCase()))
			.toList();
	}

	@Transactional(readOnly = true)
	public FriendSearchResponse searchByEmail(UUID requesterId, String email) {
		if (email == null || email.trim().isEmpty()) {
			return new FriendSearchResponse(List.of());
		}
		Optional<User> parent = userRepository.findByEmailIgnoreCase(email.trim());
		if (parent.isEmpty()) {
			return new FriendSearchResponse(List.of());
		}
		Set<UUID> acceptedFriendIds = friendshipRepository
			.findByStatusAndAccountId(FriendshipStatus.ACCEPTED, requesterId)
			.stream()
			.map((friendship) -> resolveFriendAccount(friendship, requesterId).getId())
			.collect(Collectors.toSet());
		Set<UUID> pendingFriendIds = friendshipRepository
			.findByAccountIdAndStatusIn(requesterId, List.of(FriendshipStatus.PENDING))
			.stream()
			.map((friendship) -> resolveFriendAccount(friendship, requesterId).getId())
			.collect(Collectors.toSet());
		User user = parent.get();
		List<FriendSearchResultResponse> results = new ArrayList<>();
		if (!acceptedFriendIds.contains(user.getId()) && !user.getId().equals(requesterId)) {
			results.add(toSearchResult(user, "user", pendingFriendIds));
		}
		for (Child child : user.getChildren()) {
			if (!acceptedFriendIds.contains(child.getId()) && !child.getId().equals(requesterId)) {
				results.add(toSearchResult(child, "child", pendingFriendIds));
			}
		}
		results.sort(Comparator.comparing((FriendSearchResultResponse item) -> item.getDisplayName().toLowerCase()));
		return new FriendSearchResponse(results);
	}

	@Transactional
	public FriendRequestResponse createRequest(UUID requesterId, UUID addresseeId) {
		if (requesterId.equals(addresseeId)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot request friendship with yourself.");
		}
		boolean addresseeIsChild = childRepository.existsById(addresseeId);
		boolean autoAccept = addresseeIsChild && userRepository.isParentOfChild(requesterId, addresseeId);
		Account requester = accountRepository
			.findById(requesterId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requester account not found."));
		Account addressee = accountRepository
			.findById(addresseeId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Addressee account not found."));
		Optional<AccountFriendship> existing = friendshipRepository
			.findByRequesterIdAndAddresseeIdOrRequesterIdAndAddresseeId(
				requesterId,
				addresseeId,
				addresseeId,
				requesterId
			);
		AccountFriendship friendship;
		if (existing.isPresent()) {
			friendship = existing.get();
			if (autoAccept && friendship.getStatus() == FriendshipStatus.PENDING) {
				friendship.setStatus(FriendshipStatus.ACCEPTED);
				friendship.setDecidedAt(Instant.now());
				friendship.setDecidedByParentUsername(resolveParentUsername(requesterId));
			} else if (friendship.getStatus() != FriendshipStatus.REJECTED) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Friendship already exists.");
			}
			if (friendship.getStatus() == FriendshipStatus.REJECTED) {
				friendship.setRequester(requester);
				friendship.setAddressee(addressee);
				friendship.setStatus(autoAccept ? FriendshipStatus.ACCEPTED : FriendshipStatus.PENDING);
				friendship.setRequestedAt(Instant.now());
				friendship.setDecidedAt(autoAccept ? Instant.now() : null);
				friendship.setDecidedByParentUsername(autoAccept ? resolveParentUsername(requesterId) : null);
			}
		} else {
			friendship = new AccountFriendship();
			friendship.setRequester(requester);
			friendship.setAddressee(addressee);
			friendship.setStatus(autoAccept ? FriendshipStatus.ACCEPTED : FriendshipStatus.PENDING);
			friendship.setRequestedAt(Instant.now());
			friendship.setDecidedAt(autoAccept ? Instant.now() : null);
			friendship.setDecidedByParentUsername(autoAccept ? resolveParentUsername(requesterId) : null);
		}
		AccountFriendship saved = friendshipRepository.save(friendship);
		return toFriendRequestResponse(saved);
	}

	@Transactional(readOnly = true)
	public List<FriendRequestResponse> listPendingForAccount(UUID accountId) {
		Optional<User> parent = userRepository.findById(accountId);
		if (parent.isPresent()) {
			Set<UUID> addresseeIds = parent.get().getChildren().stream()
				.map(Account::getId)
				.collect(Collectors.toSet());
			addresseeIds.add(accountId);
			return friendshipRepository
				.findByStatusAndAddresseeIdIn(FriendshipStatus.PENDING, addresseeIds)
				.stream()
				.map(this::toFriendRequestResponse)
				.toList();
		}
		return friendshipRepository
			.findByStatusAndAddresseeIdIn(FriendshipStatus.PENDING, List.of(accountId))
			.stream()
			.map(this::toFriendRequestResponse)
			.toList();
	}

	@Transactional
	public FriendRequestResponse acceptRequest(UUID requestId, UUID actorId, String actorUsername) {
		return decideRequest(requestId, actorId, actorUsername, true);
	}

	@Transactional
	public FriendRequestResponse rejectRequest(UUID requestId, UUID actorId, String actorUsername) {
		return decideRequest(requestId, actorId, actorUsername, false);
	}

	private FriendRequestResponse decideRequest(UUID requestId, UUID actorId, String actorUsername, boolean accept) {
		AccountFriendship friendship = friendshipRepository
			.findById(requestId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found."));
		if (friendship.getStatus() != FriendshipStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend request already handled.");
		}
		Account addressee = friendship.getAddressee();
		if (!canDecide(addressee, actorId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to decide this request.");
		}
		friendship.setStatus(accept ? FriendshipStatus.ACCEPTED : FriendshipStatus.REJECTED);
		friendship.setDecidedAt(Instant.now());
		friendship.setDecidedByParentUsername(actorUsername);
		AccountFriendship saved = friendshipRepository.save(friendship);
		return toFriendRequestResponse(saved);
	}

	private boolean canDecide(Account addressee, UUID actorId) {
		if (childRepository.existsById(addressee.getId())) {
			return userRepository.isParentOfChild(actorId, addressee.getId());
		}
		return addressee.getId().equals(actorId);
	}

	private String resolveParentUsername(UUID parentId) {
		return userRepository.findById(parentId).map(User::getLogin).orElse(null);
	}

	private Account resolveFriendAccount(AccountFriendship friendship, UUID accountId) {
		if (friendship.getRequester().getId().equals(accountId)) {
			return friendship.getAddressee();
		}
		return friendship.getRequester();
	}

	private FriendListItemResponse toFriendListItem(Account account) {
		return new FriendListItemResponse(
			account.getId(),
			account.getDisplayName(),
			mediaUrlResolver.resolveAvatarUrl(account.getAvatarFileName())
		);
	}

	private FriendSearchResultResponse toSearchResult(Account account, String type, Set<UUID> pendingFriendIds) {
		String status = pendingFriendIds.contains(account.getId()) ? FriendshipStatus.PENDING.name() : null;
		return new FriendSearchResultResponse(
			account.getId(),
			account.getDisplayName(),
			mediaUrlResolver.resolveAvatarUrl(account.getAvatarFileName()),
			type,
			status
		);
	}

	private FriendRequestResponse toFriendRequestResponse(AccountFriendship friendship) {
		Account requester = friendship.getRequester();
		Account addressee = friendship.getAddressee();
		return new FriendRequestResponse(
			friendship.getId(),
			requester.getId(),
			requester.getDisplayName(),
			mediaUrlResolver.resolveAvatarUrl(requester.getAvatarFileName()),
			addressee.getId(),
			addressee.getDisplayName(),
			mediaUrlResolver.resolveAvatarUrl(addressee.getAvatarFileName()),
			friendship.getStatus().name(),
			friendship.getRequestedAt()
		);
	}
}
