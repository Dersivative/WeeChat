export type ThreadListItem = {
  threadId: string
  title: string
  lastMessageText: string
  lastMessageAt: string | null
  unread: boolean
  avatarUrls: string[]
  memberAccountIds: string[]
}

export type ThreadListResponse = {
  threads: ThreadListItem[]
}
