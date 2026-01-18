package com.weetalk.chat.mongo;

import java.util.UUID;

public class ThreadMember {
	private AccountType type;
	private UUID accountId;

	public AccountType getType() {
		return type;
	}

	public void setType(AccountType type) {
		this.type = type;
	}

	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(UUID accountId) {
		this.accountId = accountId;
	}
}
