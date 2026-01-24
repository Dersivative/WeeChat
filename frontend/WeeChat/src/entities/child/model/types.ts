export type ModerationLevel = 'NONE' | 'AUTOMATED' | 'MANUAL'

export type ChildProfile = {
  id: string
  displayName: string
  avatarUrl?: string | null
  moderationLevel: ModerationLevel
}
