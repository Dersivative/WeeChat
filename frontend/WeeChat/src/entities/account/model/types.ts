export type LoginResponse = {
  accountId: string
  login: string
  twoFactorEnabled: boolean
  avatarUrl?: string | null
  accessToken: string
  refreshToken: string
}

export type ChildLoginResponse = {
  accountId: string
  displayName: string
  avatarUrl?: string | null
  accessToken: string
}

export type AccountType = 'user' | 'child'

export type AuthState = {
  login: string
  accessToken: string
  refreshToken: string
  accountType: AccountType
  profile: LoginResponse
}
