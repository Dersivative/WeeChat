package com.weetalk.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "child")
public class Child {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 100)
	private String displayName;

	@Column(length = 255)
	private String loginCodeHash;

	private Instant loginCodeExpiresAt;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private ChildLoginCodeType loginCodeType;

	@ManyToMany(mappedBy = "children", fetch = FetchType.LAZY)
	private Set<User> parents = new HashSet<>();

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

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

	public Set<User> getParents() {
		return parents;
	}

	public void setParents(Set<User> parents) {
		this.parents = parents;
	}
}
