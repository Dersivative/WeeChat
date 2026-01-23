package com.weetalk.chat.accounts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_friendship")
public class AccountFriendship {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requester_account_id", nullable = false)
	private Account requester;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "addressee_account_id", nullable = false)
	private Account addressee;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private FriendshipStatus status = FriendshipStatus.PENDING;

	@Column(nullable = false)
	private Instant requestedAt;

	private Instant decidedAt;

	@Column(length = 100)
	private String decidedByParentUsername;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Account getRequester() {
		return requester;
	}

	public void setRequester(Account requester) {
		this.requester = requester;
	}

	public Account getAddressee() {
		return addressee;
	}

	public void setAddressee(Account addressee) {
		this.addressee = addressee;
	}

	public FriendshipStatus getStatus() {
		return status;
	}

	public void setStatus(FriendshipStatus status) {
		this.status = status;
	}

	public Instant getRequestedAt() {
		return requestedAt;
	}

	public void setRequestedAt(Instant requestedAt) {
		this.requestedAt = requestedAt;
	}

	public Instant getDecidedAt() {
		return decidedAt;
	}

	public void setDecidedAt(Instant decidedAt) {
		this.decidedAt = decidedAt;
	}

	public String getDecidedByParentUsername() {
		return decidedByParentUsername;
	}

	public void setDecidedByParentUsername(String decidedByParentUsername) {
		this.decidedByParentUsername = decidedByParentUsername;
	}
}
