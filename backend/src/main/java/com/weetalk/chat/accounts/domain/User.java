package com.weetalk.chat.accounts.domain;

import com.weetalk.chat.children.domain.Child;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "app_user")
public class User extends Account {
	/** Encja reprezentuje konto samodzielne - pełnoprawne. */

	@Column(nullable = false, unique = true, length = 100)
	private String login;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(nullable = false, length = 255)
	private String passwordHash;

	@Column(nullable = false)
	private boolean twoFactorEnabled;

	@Column(length = 255)
	private String twoFactorSecret;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "parent_child",
		joinColumns = @JoinColumn(name = "parent_id"),
		inverseJoinColumns = @JoinColumn(name = "child_id")
	)
	private Set<Child> children = new HashSet<>();

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public boolean isTwoFactorEnabled() {
		return twoFactorEnabled;
	}

	public void setTwoFactorEnabled(boolean twoFactorEnabled) {
		this.twoFactorEnabled = twoFactorEnabled;
	}

	public String getTwoFactorSecret() {
		return twoFactorSecret;
	}

	public void setTwoFactorSecret(String twoFactorSecret) {
		this.twoFactorSecret = twoFactorSecret;
	}

	public Set<Child> getChildren() {
		return children;
	}

	public void setChildren(Set<Child> children) {
		this.children = children;
	}
}
