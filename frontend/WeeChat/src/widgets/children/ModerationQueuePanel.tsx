import { useCallback, useEffect, useState } from 'react'
import type { ChildProfile } from '../../entities/child'
import type { ModerationQueueResponse, ModerationThread, ModerationMessage } from '../../entities/moderation'
import type { AuthFetch } from '../../shared/apiClient'

type ModerationQueuePanelProps = {
  apiBaseUrl: string
  authFetch: AuthFetch
}

function ModerationQueuePanel({ apiBaseUrl, authFetch }: ModerationQueuePanelProps) {
  const [children, setChildren] = useState<ChildProfile[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [threads, setThreads] = useState<ModerationThread[]>([])
  const [selectedThreadId, setSelectedThreadId] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadChildren = useCallback(async () => {
    setError(null)
    try {
      const response = await authFetch(`${apiBaseUrl}/api/children`)
      if (!response.ok) {
        throw new Error('Could not load children.')
      }
      const payload = (await response.json()) as ChildProfile[]
      setChildren(payload)
      if (!selectedId && payload.length > 0) {
        setSelectedId(payload[0].id)
      }
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Could not load children.')
    }
  }, [apiBaseUrl, authFetch, selectedId])

  const loadQueue = useCallback(
    async (childId: string) => {
    setLoading(true)
    setError(null)
    try {
      const response = await authFetch(`${apiBaseUrl}/api/children/${childId}/moderation-queue`)
      if (!response.ok) {
        throw new Error('Could not load moderation queue.')
      }
      const payload = (await response.json()) as ModerationQueueResponse
      setThreads(payload.threads)
      if (payload.threads.length > 0) {
        setSelectedThreadId((prev) => prev ?? payload.threads[0].threadId)
      } else {
        setSelectedThreadId(null)
      }
    } catch (queueError) {
      setError(queueError instanceof Error ? queueError.message : 'Could not load moderation queue.')
    } finally {
      setLoading(false)
    }
    },
    [apiBaseUrl, authFetch]
  )

  useEffect(() => {
    void loadChildren()
  }, [loadChildren])

  useEffect(() => {
    if (selectedId) {
      void loadQueue(selectedId)
    } else {
      setThreads([])
      setSelectedThreadId(null)
    }
  }, [loadQueue, selectedId])

  const handleDecision = async (messageId: string, action: 'approve' | 'reject') => {
    setError(null)
    try {
      const response = await authFetch(`${apiBaseUrl}/api/messages/${messageId}/${action}`, {
        method: 'POST',
      })
      if (!response.ok) {
        throw new Error('Could not update moderation status.')
      }
      const payload = (await response.json()) as ModerationMessage
      setThreads((prev) =>
        prev.map((thread) => ({
          ...thread,
          messages: thread.messages.map((message) =>
            message.id === payload.id ? { ...message, status: payload.status } : message
          ),
        }))
      )
    } catch (decisionError) {
      setError(decisionError instanceof Error ? decisionError.message : 'Could not update moderation status.')
    }
  }

  const renderStatusBadge = (message: ModerationMessage) => {
    if (message.suggestedStatus) {
      return (
        <span className={`llm-badge ${message.suggestedStatus.toLowerCase()}`}>
          LLM: {message.suggestedStatus.toLowerCase()}
        </span>
      )
    }
    return <span className="llm-badge neutral">LLM: n/a</span>
  }

  const selectedThread = threads.find((thread) => thread.threadId === selectedThreadId) ?? null

  return (
    <section className="panel-card">
      <header className="panel-header">
        <div>
          <p className="section-label">Moderation</p>
          <h2>Moderate Messages</h2>
        </div>
        <button
          className="ghost"
          type="button"
          onClick={() => (selectedId ? loadQueue(selectedId) : loadChildren())}
          disabled={loading}
        >
          Refresh
        </button>
      </header>

      {error ? <p className="form-error">{error}</p> : null}
      {loading ? <p className="status">Loading moderation queue...</p> : null}

      <div className="panel-split">
        <div className="panel-sidebar">
          <p className="panel-meta">Choose child</p>
          {children.length === 0 && !loading ? <p className="status subtle">No children yet.</p> : null}
          {children.map((child) => (
            <button
              key={child.id}
              type="button"
              className={`panel-item ${child.id === selectedId ? 'active' : ''}`}
              onClick={() => setSelectedId(child.id)}
            >
              {child.displayName}
            </button>
          ))}
          <p className="panel-meta">Choose thread</p>
          {threads.length === 0 && !loading ? <p className="status subtle">No threads yet.</p> : null}
          {threads.map((thread) => (
            <button
              key={thread.threadId}
              type="button"
              className={`panel-item ${thread.threadId === selectedThreadId ? 'active' : ''}`}
              onClick={() => setSelectedThreadId(thread.threadId)}
            >
              {thread.title}
            </button>
          ))}
        </div>
        <div className="panel-content">
          {!selectedThread && !loading ? (
            <p className="status subtle">Select a thread to moderate.</p>
          ) : null}
          {selectedThread ? (
            <article className="moderation-thread">
              <div className="moderation-thread-header">
                <div className="thread-avatar">
                  {selectedThread.avatarUrls.length > 1 ? (
                    <div className="avatar-collage">
                      {selectedThread.avatarUrls.slice(0, 3).map((url, index) => (
                        <img
                          key={`${selectedThread.threadId}-${index}`}
                          src={url}
                          alt=""
                          className="avatar-collage-item"
                        />
                      ))}
                    </div>
                  ) : selectedThread.avatarUrls.length === 1 ? (
                    <img className="avatar" src={selectedThread.avatarUrls[0]} alt="" />
                  ) : (
                    <div className="avatar avatar-fallback">
                      {selectedThread.title.slice(0, 2).toUpperCase()}
                    </div>
                  )}
                </div>
                <div>
                  <h3>{selectedThread.title}</h3>
                  <p className="panel-meta">{selectedThread.messages.length} messages</p>
                </div>
              </div>
              <div className="moderation-messages">
                {selectedThread.messages.map((message) => {
                  const isOutgoing = message.senderAccountId === selectedId
                  const statusClass = isOutgoing ? 'outgoing' : message.status.toLowerCase()
                  return (
                    <div key={message.id} className={`moderation-message ${statusClass}`}>
                      <div className="moderation-message-body">
                        <p>{message.text}</p>
                        <div className="moderation-message-meta">
                          {isOutgoing ? <span className="llm-badge neutral">Outgoing</span> : renderStatusBadge(message)}
                          <span>{new Date(message.createdAt).toLocaleTimeString()}</span>
                        </div>
                      </div>
                      {!isOutgoing && message.status === 'PENDING' ? (
                        <div className="moderation-actions">
                          <button
                            className="icon-button approve"
                            type="button"
                            title="Approve"
                            onClick={() => handleDecision(message.id, 'approve')}
                          >
                            ✓
                          </button>
                          <button
                            className="icon-button reject"
                            type="button"
                            title="Reject"
                            onClick={() => handleDecision(message.id, 'reject')}
                          >
                            ×
                          </button>
                        </div>
                      ) : null}
                    </div>
                  )
                })}
              </div>
            </article>
          ) : null}
        </div>
      </div>
    </section>
  )
}

export default ModerationQueuePanel
