export type LoginResponse = {
  accountId: string
  login: string
  twoFactorEnabled: boolean
  avatarUrl?: string | null
  accessToken: string
  refreshToken: string
}

export type ThreadListItem = {
  threadId: string
  title: string
  lastMessageText: string
  lastMessageAt: string | null
  unread: boolean
  avatarUrls: string[]
}

export type ThreadListResponse = {
  threads: ThreadListItem[]
}

export type AuthState = {
  login: string
  accessToken: string
  refreshToken: string
  profile: LoginResponse
}
