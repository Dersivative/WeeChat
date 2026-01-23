export type MessageItem = {
  id: string
  threadId: string
  senderAccountId: string
  text: string
  createdAt: string
}

export type MessageListResponse = {
  messages: MessageItem[]
  hasMore: boolean
  nextBefore?: string | null
}
