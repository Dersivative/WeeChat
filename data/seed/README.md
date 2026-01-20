# Seed data

This folder contains mock data for PostgreSQL and MongoDB. LLM was used to generate the mock data.

## PostgreSQL

Run the SQL seed via `psql` so the script can create the database and then connect:

```bash
psql "postgresql://weechat_user:weechat_password@localhost:54321/postgres" \
	-f data/seed/postgres_seed.sql
```

## MongoDB

Run the Mongo seed via `mongosh`:

```bash
mongosh "mongodb://weechat_root:weechat_password@localhost:21345/admin" \
	data/seed/mongo_seed.js
```
