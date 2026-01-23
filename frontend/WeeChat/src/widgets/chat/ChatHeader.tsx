type ChatHeaderProps = {
  login: string
  avatarUrl?: string | null
  menuOpen: boolean
  onMenuOpen: () => void
  onLogout: () => void
}

function ChatHeader({ login, avatarUrl, menuOpen, onMenuOpen, onLogout }: ChatHeaderProps) {
  return (
    <header className="chat-header">
      <div className="chat-header-text">
        <div className="badge">WeeChat</div>
        <h1>Chats</h1>
        <p className="subtitle">Only the threads you belong to are listed.</p>
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
