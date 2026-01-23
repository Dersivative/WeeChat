import type { ThreadListItem } from '../../entities/thread'

type ThreadListProps = {
  threads: ThreadListItem[]
  loading: boolean
  error: string | null
  selectedThreadId: string | null
  onSelectThread: (threadId: string) => void
}

function ThreadList({ threads, loading, error, selectedThreadId, onSelectThread }: ThreadListProps) {
  const formatTimestamp = (value?: string | null) => {
    if (!value) {
      return 'No activity yet'
    }
    return new Date(value).toLocaleString()
  }

  const fallbackTitle = (title: string) => title.slice(0, 2).toUpperCase()

  return (
    <section className="thread-list">
      {loading ? <p className="status">Loading chats...</p> : null}
      {error ? <p className="form-error">{error}</p> : null}
      {!loading && !error && threads.length === 0 ? (
        <p className="status">No chats yet. Start a new conversation.</p>
      ) : null}
      {threads.map((thread) => (
        <button
          className={`thread-card ${thread.unread ? 'unread' : ''} ${
            thread.threadId === selectedThreadId ? 'active' : ''
          }`}
          key={thread.threadId}
          type="button"
          onClick={() => onSelectThread(thread.threadId)}
        >
          <div className="thread-avatar">
            {thread.avatarUrls.length > 1 ? (
              <div className="avatar-collage">
                {thread.avatarUrls.slice(0, 3).map((url, index) => (
                  <img
                    key={`${thread.threadId}-${index}`}
                    src={url}
                    alt=""
                    className="avatar-collage-item"
                  />
                ))}
              </div>
            ) : thread.avatarUrls.length === 1 ? (
              <img className="avatar" src={thread.avatarUrls[0]} alt="" />
            ) : (
              <div className="avatar avatar-fallback">{fallbackTitle(thread.title)}</div>
            )}
          </div>
          <div className="thread-body">
            <h3>{thread.title}</h3>
            <p className="thread-preview">{thread.lastMessageText}</p>
          </div>
          <div className="thread-meta">
            <span>{formatTimestamp(thread.lastMessageAt)}</span>
          </div>
        </button>
      ))}
    </section>
  )
}

export default ThreadList
