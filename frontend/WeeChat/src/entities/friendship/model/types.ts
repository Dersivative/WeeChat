export type FriendshipStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

export type FriendListItem = {
  accountId: string
  displayName: string
  avatarUrl?: string | null
}

export type FriendSearchResult = {
  accountId: string
  displayName: string
  avatarUrl?: string | null
  accountType: 'user' | 'child'
  friendshipStatus?: FriendshipStatus | null
}

export type FriendSearchResponse = {
  results: FriendSearchResult[]
}

export type FriendRequestItem = {
  id: string
  requesterAccountId: string
  requesterDisplayName: string
  requesterAvatarUrl?: string | null
  addresseeAccountId: string
  addresseeDisplayName: string
  addresseeAvatarUrl?: string | null
  status: FriendshipStatus
  requestedAt: string
}
