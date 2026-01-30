package com.weetalk.chat.threads.application;

import com.weetalk.chat.accounts.domain.Account;
import com.weetalk.chat.accounts.infrastructure.AccountRepository;
import com.weetalk.chat.media.MediaUrlResolver;
import com.weetalk.chat.messages.domain.MessageDeliveryState;
import com.weetalk.chat.messages.domain.MessageDeliveryStatus;
import com.weetalk.chat.messages.infrastructure.mongo.MessageDoc;
import com.weetalk.chat.messages.infrastructure.mongo.MessageRepository;
import com.weetalk.chat.threads.api.dto.ThreadListItemResponse;
import com.weetalk.chat.threads.api.dto.ThreadListResponse;
import com.weetalk.chat.threads.infrastructure.mongo.ThreadDoc;
import com.weetalk.chat.threads.infrastructure.mongo.ThreadMember;
import com.weetalk.chat.threads.infrastructure.mongo.ThreadRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ThreadListService {
	private static final String MESSAGE_UNAVAILABLE = "Message unavailable";
	private static final String EMPTY_CHAT = "No messages yet";

	private final ThreadRepository threadRepository;
	private final MessageRepository messageRepository;
	private final AccountRepository accountRepository;
	private final MediaUrlResolver mediaUrlResolver;

	public ThreadListService(
		ThreadRepository threadRepository,
		MessageRepository messageRepository,
		AccountRepository accountRepository,
		MediaUrlResolver mediaUrlResolver
	) {
		this.threadRepository = threadRepository;
		this.messageRepository = messageRepository;
		this.accountRepository = accountRepository;
		this.mediaUrlResolver = mediaUrlResolver;
	}

	public ThreadListResponse listThreads(UUID viewerAccountId) {
		List<ThreadDoc> threads = threadRepository
			.findByMembersAccountIdAndMembersLeftAtIsNullOrderByLastMessageAtDesc(viewerAccountId);

		List<UUID> accountIds = threads.stream()
			.flatMap(thread -> activeMembers(thread).stream())
			.map(ThreadMember::getAccountId)
			.filter(Objects::nonNull)
			.distinct()
			.toList();

		Map<UUID, Account> accounts = accountRepository.findAllById(accountIds)
			.stream()
			.collect(Collectors.toMap(Account::getId, Function.identity()));

		List<ThreadListItemResponse> items = new ArrayList<>();
		for (ThreadDoc thread : threads) {
			List<ThreadMember> members = activeMembers(thread);
			MessageDoc lastMessage = messageRepository.findTopByThreadIdOrderByCreatedAtDesc(thread.getId());
			items.add(toListItem(thread, members, accounts, viewerAccountId, lastMessage));
		}

		items.sort(Comparator.comparing(ThreadListItemResponse::getLastMessageAt, Comparator.nullsLast(Comparator.naturalOrder()))
			.reversed());

		return new ThreadListResponse(items);
	}

	private ThreadListItemResponse toListItem(
		ThreadDoc thread,
		List<ThreadMember> members,
		Map<UUID, Account> accounts,
		UUID viewerAccountId,
		MessageDoc lastMessage
	) {
		String title = resolveTitle(thread, members, accounts, viewerAccountId);
		List<String> avatarUrls = resolveAvatars(members, accounts, viewerAccountId);
		List<String> memberAccountIds = members.stream()
			.map(ThreadMember::getAccountId)
			.filter(Objects::nonNull)
			.map(UUID::toString)
			.toList();
		String lastMessageText = resolveLastMessageText(lastMessage);
		Instant lastMessageAt = resolveLastMessageAt(thread, lastMessage);
		boolean unread = isUnread(lastMessage, viewerAccountId);

		return new ThreadListItemResponse(
			thread.getId(),
			title,
			lastMessageText,
			lastMessageAt,
			unread,
			avatarUrls,
			memberAccountIds
		);
	}

	private List<ThreadMember> activeMembers(ThreadDoc thread) {
		return thread.getMembers()
			.stream()
			.filter(member -> member.getLeftAt() == null)
			.toList();
	}

	private String resolveTitle(
		ThreadDoc thread,
		List<ThreadMember> members,
		Map<UUID, Account> accounts,
		UUID viewerAccountId
	) {
		if (thread.getCustomTitle() != null && !thread.getCustomTitle().isBlank()) {
			return thread.getCustomTitle();
		}

		if (members.size() == 2) {
			for (ThreadMember member : members) {
				if (!viewerAccountId.equals(member.getAccountId())) {
					return displayNameFor(accounts, member.getAccountId());
				}
			}
		}

		if (!members.isEmpty()) {
			ThreadMember firstMember = members.get(0);
			int remaining = Math.max(0, members.size() - 2);
			if (members.size() >= 3) {
				return displayNameFor(accounts, firstMember.getAccountId()) + " + " + remaining + " osób";
			}

			return displayNameFor(accounts, firstMember.getAccountId());
		}

		return "Chat";
	}

	private List<String> resolveAvatars(
		List<ThreadMember> members,
		Map<UUID, Account> accounts,
		UUID viewerAccountId
	) {
		List<String> avatars = new ArrayList<>();

		if (members.size() == 2) {
			for (ThreadMember member : members) {
				if (!viewerAccountId.equals(member.getAccountId())) {
					String avatarUrl = avatarFor(accounts, member.getAccountId());
					if (avatarUrl != null) {
						avatars.add(avatarUrl);
					}
					return avatars;
				}
			}
		}

		for (int i = 0; i < Math.min(3, members.size()); i += 1) {
			String avatarUrl = avatarFor(accounts, members.get(i).getAccountId());
			if (avatarUrl != null) {
				avatars.add(avatarUrl);
			}
		}

		return avatars;
	}

	private String resolveLastMessageText(MessageDoc lastMessage) {
		if (lastMessage == null) {
			return EMPTY_CHAT;
		}

		if (lastMessage.getDeletedAt() != null || lastMessage.getText() == null || lastMessage.getText().isBlank()) {
			return MESSAGE_UNAVAILABLE;
		}

		return lastMessage.getText();
	}

	private Instant resolveLastMessageAt(ThreadDoc thread, MessageDoc lastMessage) {
		if (lastMessage != null && lastMessage.getCreatedAt() != null) {
			return lastMessage.getCreatedAt();
		}

		return thread.getLastMessageAt();
	}

	private boolean isUnread(MessageDoc lastMessage, UUID viewerAccountId) {
		if (lastMessage == null || viewerAccountId == null) {
			return false;
		}

		if (viewerAccountId.equals(lastMessage.getSenderAccountId())) {
			return false;
		}

		List<MessageDeliveryStatus> deliveryStatuses = lastMessage.getDeliveryStatuses();
		if (deliveryStatuses == null || deliveryStatuses.isEmpty()) {
			return true;
		}

		return deliveryStatuses.stream()
			.filter(status -> viewerAccountId.equals(status.getAccountId()))
			.map(MessageDeliveryStatus::getState)
			.findFirst()
			.map(state -> state != MessageDeliveryState.READ)
			.orElse(true);
	}

	private String displayNameFor(Map<UUID, Account> accounts, UUID accountId) {
		Account account = accounts.get(accountId);
		if (account != null && account.getDisplayName() != null && !account.getDisplayName().isBlank()) {
			return account.getDisplayName();
		}
		return "Unknown";
	}

	private String avatarFor(Map<UUID, Account> accounts, UUID accountId) {
		Account account = accounts.get(accountId);
		if (account != null && account.getAvatarFileName() != null && !account.getAvatarFileName().isBlank()) {
			return mediaUrlResolver.resolveAvatarUrl(account.getAvatarFileName());
		}
		return null;
	}
}
