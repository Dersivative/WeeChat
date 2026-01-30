import type { ModerationLevel } from '../../child'

export type ModerationStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export type ModerationMessage = {
  id: string
  threadId: string
  senderAccountId: string
  text: string
  createdAt: string
  status: ModerationStatus
  suggestedStatus?: ModerationStatus | null
  score?: number | null
  label?: string | null
}

export type ModerationThread = {
  threadId: string
  title: string
  lastMessageAt: string | null
  avatarUrls: string[]
  messages: ModerationMessage[]
}

export type ModerationQueueResponse = {
  threads: ModerationThread[]
}

export type ModerationLevelOption = {
  value: ModerationLevel
  label: string
  description: string
}
