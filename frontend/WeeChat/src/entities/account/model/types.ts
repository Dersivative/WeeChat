export type LoginResponse = {
  accountId: string
  login: string
  twoFactorEnabled: boolean
  avatarUrl?: string | null
}

export type ChildLoginResponse = {
  accountId: string
  displayName: string
  avatarUrl?: string | null
}

export type AccountType = 'user' | 'child'

export type AuthState = {
  accountType: AccountType
  profile: LoginResponse
}
