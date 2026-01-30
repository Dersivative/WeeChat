const dbInstance = db.getSiblingDB("weechat");

const existingThreads = dbInstance.threads.countDocuments();
const existingMessages = dbInstance.messages.countDocuments();
if (existingThreads > 0 || existingMessages > 0) {
  print("Seed skipped: threads/messages already exist.");
} else {
  // dbInstance.threads.deleteMany({});
  // dbInstance.messages.deleteMany({});

  dbInstance.threads.insertMany([
	{
		_id: "thread-001",
		members: [
			{
				accountId: UUID("11111111-1111-1111-1111-111111111111"),
				joinedAt: ISODate("2026-01-19T08:00:00Z"),
				leftAt: null,
				lastReadAt: ISODate("2026-01-20T09:10:00Z")
			},
			{
				accountId: UUID("33333333-3333-3333-3333-333333333333"),
				joinedAt: ISODate("2026-01-19T08:05:00Z"),
				leftAt: null,
				lastReadAt: ISODate("2026-01-20T09:05:00Z")
			}
		],
		createdAt: ISODate("2026-01-19T08:00:00Z"),
		lastMessageAt: ISODate("2026-01-20T09:02:00Z")
	},
	{
		_id: "thread-002",
		members: [
			{
				accountId: UUID("33333333-3333-3333-3333-333333333333"),
				joinedAt: ISODate("2026-01-18T15:00:00Z"),
				leftAt: null,
				lastReadAt: ISODate("2026-01-20T08:00:00Z")
			},
			{
				accountId: UUID("44444444-4444-4444-4444-444444444444"),
				joinedAt: ISODate("2026-01-18T15:00:00Z"),
				leftAt: null,
				lastReadAt: ISODate("2026-01-20T08:05:00Z")
			}
		],
		createdAt: ISODate("2026-01-18T15:00:00Z"),
		lastMessageAt: ISODate("2026-01-20T08:40:00Z")
	},
	{
		_id: "thread-003",
		members: [
			{
				accountId: UUID("11111111-1111-1111-1111-111111111111"),
				joinedAt: ISODate("2026-01-20T13:00:00Z"),
				leftAt: null,
				lastReadAt: ISODate("2026-01-20T13:10:00Z")
			},
			{
				accountId: UUID("22222222-2222-2222-2222-222222222222"),
				joinedAt: ISODate("2026-01-20T13:00:00Z"),
				leftAt: null,
				lastReadAt: ISODate("2026-01-20T13:12:00Z")
			}
		],
		createdAt: ISODate("2026-01-20T13:00:00Z"),
		lastMessageAt: ISODate("2026-01-20T13:12:00Z")
	}
]);

  dbInstance.messages.insertMany([
	{
		_id: "msg-001",
		threadId: "thread-001",
		senderAccountId: UUID("11111111-1111-1111-1111-111111111111"),
		text: "Welcome to WeeChat!",
		attachments: [],
		deliveryStatuses: [
			{
				accountId: UUID("33333333-3333-3333-3333-333333333333"),
				state: "DELIVERED",
				updatedAt: ISODate("2026-01-20T09:00:30Z")
			}
		],
		moderationDecision: {
			status: "APPROVED",
			decidedByParentUsername: "alice",
			decidedAt: ISODate("2026-01-20T09:00:00Z"),
			reason: "Greeting",
			modelData: {
				toxicity: 0.01
			}
		},
		deletedAt: null,
		deletedByAccountId: null,
		createdAt: ISODate("2026-01-20T09:00:00Z")
	},
	{
		_id: "msg-002",
		threadId: "thread-001",
		senderAccountId: UUID("33333333-3333-3333-3333-333333333333"),
		text: "Thanks!",
		attachments: [],
		deliveryStatuses: [
			{
				accountId: UUID("11111111-1111-1111-1111-111111111111"),
				state: "READ",
				updatedAt: ISODate("2026-01-20T09:03:00Z")
			}
		],
		moderationDecision: {
			status: "APPROVED",
			decidedByParentUsername: "alice",
			decidedAt: ISODate("2026-01-20T09:02:30Z"),
			reason: "Friendly reply",
			modelData: {
				toxicity: 0.0
			}
		},
		deletedAt: null,
		deletedByAccountId: null,
		createdAt: ISODate("2026-01-20T09:02:00Z")
	},
	{
		_id: "msg-003",
		threadId: "thread-002",
		senderAccountId: UUID("33333333-3333-3333-3333-333333333333"),
		text: "Check out this photo!",
		attachments: [
			{
				kind: "IMAGE",
				filename: "photo-001.png",
				contentType: "image/png",
				sizeBytes: 24567
			}
		],
		deliveryStatuses: [
			{
				accountId: UUID("44444444-4444-4444-4444-444444444444"),
				state: "DELIVERED",
				updatedAt: ISODate("2026-01-20T08:31:00Z")
			}
		],
		moderationDecision: {
			status: "PENDING",
			decidedByParentUsername: null,
			decidedAt: null,
			reason: null,
			modelData: {
				autoFlag: false
			}
		},
		deletedAt: null,
		deletedByAccountId: null,
		createdAt: ISODate("2026-01-20T08:30:00Z")
	},
	{
		_id: "msg-004",
		threadId: "thread-002",
		senderAccountId: UUID("44444444-4444-4444-4444-444444444444"),
		text: "Cool! Here is an audio note.",
		attachments: [
			{
				kind: "AUDIO",
				filename: "audio-001.m4a",
				contentType: "audio/mp4",
				sizeBytes: 73421
			}
		],
		deliveryStatuses: [
			{
				accountId: UUID("33333333-3333-3333-3333-333333333333"),
				state: "DELIVERED",
				updatedAt: ISODate("2026-01-20T08:41:00Z")
			}
		],
		moderationDecision: {
			status: "APPROVED",
			decidedByParentUsername: "alice",
			decidedAt: ISODate("2026-01-20T08:42:00Z"),
			reason: "Audio clip approved",
			modelData: {
				toxicity: 0.0
			}
		},
		deletedAt: null,
		deletedByAccountId: null,
		createdAt: ISODate("2026-01-20T08:40:00Z")
	}
  ]);
}
