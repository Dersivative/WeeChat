type ChatHeaderProps = {
  login: string
  avatarUrl?: string | null
  menuOpen: boolean
  viewMode: 'chats' | 'friends'
  onMenuOpen: () => void
  onLogout: () => void
}

function ChatHeader({
  login,
  avatarUrl,
  menuOpen,
  viewMode,
  onMenuOpen,
  onLogout,
}: ChatHeaderProps) {
  const isFriends = viewMode === 'friends'
  const title = isFriends ? 'Friends' : 'Chats'
  const subtitle = ''

  return (
    <header className="chat-header">
      <div className="chat-header-text">
        <div className="badge">WeeChat</div>
        <h1>{title}</h1>
        {subtitle ? <p className="subtitle">{subtitle}</p> : null}
      </div>
      <div className="chat-user">
        <div className="user-meta">
          <span className="user-login">{login}</span>
          <button className="link" type="button" onClick={onLogout}>
            Sign out
          </button>
        </div>
        <button
          className="avatar-button"
          type="button"
          onClick={onMenuOpen}
          aria-haspopup="menu"
          aria-expanded={menuOpen}
        >
          {avatarUrl ? (
            <img className="avatar" src={avatarUrl} alt={`${login} avatar`} />
          ) : (
            <div className="avatar avatar-fallback">{login.slice(0, 2).toUpperCase()}</div>
          )}
        </button>
      </div>
    </header>
  )
}

export default ChatHeader
