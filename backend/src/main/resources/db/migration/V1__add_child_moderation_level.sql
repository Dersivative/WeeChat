-- Schema creation for weechat_db

CREATE TABLE account (
	id UUID PRIMARY KEY,
	avatar_url VARCHAR(500),
	display_name VARCHAR(100) NOT NULL
);

CREATE TABLE app_user (
	id UUID PRIMARY KEY REFERENCES account(id) ON DELETE CASCADE,
	login VARCHAR(100) NOT NULL UNIQUE,
	email VARCHAR(255) NOT NULL UNIQUE,
	password_hash VARCHAR(255) NOT NULL,
	two_factor_enabled BOOLEAN NOT NULL,
	two_factor_secret VARCHAR(255)
);

CREATE TABLE child (
	id UUID PRIMARY KEY REFERENCES account(id) ON DELETE CASCADE,
	login_code_hash VARCHAR(255),
	login_code_expires_at TIMESTAMPTZ,
	login_code_type VARCHAR(20),
	moderation_level VARCHAR(20) NOT NULL DEFAULT 'MANUAL'
);

CREATE TABLE parent_child (
	parent_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
	child_id UUID NOT NULL REFERENCES child(id) ON DELETE CASCADE,
	PRIMARY KEY (parent_id, child_id)
);

CREATE TABLE account_friendship (
	id UUID PRIMARY KEY,
	requester_account_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
	addressee_account_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
	status VARCHAR(20) NOT NULL,
	requested_at TIMESTAMPTZ NOT NULL,
	decided_at TIMESTAMPTZ,
	decided_by_parent_username VARCHAR(100)
);

CREATE TABLE account_block (
	id UUID PRIMARY KEY,
	blocker_account_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
	blocked_account_id UUID NOT NULL REFERENCES account(id) ON DELETE CASCADE,
	created_at TIMESTAMPTZ
);
