package com.weetalk.chat.children.domain;

import com.weetalk.chat.accounts.domain.Account;
import com.weetalk.chat.accounts.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "child")
public class Child extends Account {
	/** Encja reprezentuje konto dziecka - silnie powiązane z kontem typu User (rodzica/opiekuna) */

	@Column(length = 255)
	private String loginCodeHash;

	private Instant loginCodeExpiresAt;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private ChildLoginCodeType loginCodeType;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private ModerationLevel moderationLevel = ModerationLevel.MANUAL;

	@ManyToMany(mappedBy = "children", fetch = FetchType.LAZY)
	private Set<User> parents = new HashSet<>();

	public String getLoginCodeHash() {
		return loginCodeHash;
	}

	public void setLoginCodeHash(String loginCodeHash) {
		this.loginCodeHash = loginCodeHash;
	}

	public Instant getLoginCodeExpiresAt() {
		return loginCodeExpiresAt;
	}

	public void setLoginCodeExpiresAt(Instant loginCodeExpiresAt) {
		this.loginCodeExpiresAt = loginCodeExpiresAt;
	}

	public ChildLoginCodeType getLoginCodeType() {
		return loginCodeType;
	}

	public void setLoginCodeType(ChildLoginCodeType loginCodeType) {
		this.loginCodeType = loginCodeType;
	}

	public ModerationLevel getModerationLevel() {
		return moderationLevel == null ? ModerationLevel.MANUAL : moderationLevel;
	}

	public void setModerationLevel(ModerationLevel moderationLevel) {
		this.moderationLevel = moderationLevel;
	}

	public Set<User> getParents() {
		return parents;
	}

	public void setParents(Set<User> parents) {
		this.parents = parents;
	}
}
