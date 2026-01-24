type ChatViewSwitchProps = {
  viewMode: 'chats' | 'friends'
  onViewChange: (mode: 'chats' | 'friends') => void
}

function ChatViewSwitch({ viewMode, onViewChange }: ChatViewSwitchProps) {
  const isFriends = viewMode === 'friends'

  return (
    <div className={`switcher ${isFriends ? 'child' : ''}`}>
      <button
        className={!isFriends ? 'active' : ''}
        type="button"
        onClick={() => onViewChange('chats')}
      >
        Chats
      </button>
      <button
        className={isFriends ? 'active' : ''}
        type="button"
        onClick={() => onViewChange('friends')}
      >
        Friends
      </button>
      <span className="switch-pill" aria-hidden="true" />
    </div>
  )
}

export default ChatViewSwitch
