import { useCallback, useEffect, useState } from 'react'
import type { ChildProfile } from '../../entities/child'
import type { FriendRequestItem } from '../../entities/friendship'
import type { ModerationQueueResponse, ModerationThread, ModerationMessage } from '../../entities/moderation'
import type { AuthFetch } from '../../shared/apiClient'

type ModerationQueuePanelProps = {
  apiBaseUrl: string
  authFetch: AuthFetch
}

function ModerationQueuePanel({ apiBaseUrl, authFetch }: ModerationQueuePanelProps) {
  const [activeView, setActiveView] = useState<'chats' | 'friends'>('chats')
  const [children, setChildren] = useState<ChildProfile[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [threads, setThreads] = useState<ModerationThread[]>([])
  const [selectedThreadId, setSelectedThreadId] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [friendRequests, setFriendRequests] = useState<FriendRequestItem[]>([])
  const [friendLoading, setFriendLoading] = useState(false)
  const [friendError, setFriendError] = useState<string | null>(null)

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

  const loadFriendRequests = useCallback(async () => {
    setFriendLoading(true)
    setFriendError(null)
    try {
      const response = await authFetch(`${apiBaseUrl}/api/friends/requests/pending`)
      if (!response.ok) {
        throw new Error('Could not load friend requests.')
      }
      const payload = (await response.json()) as FriendRequestItem[]
      setFriendRequests(payload)
    } catch (friendLoadError) {
      setFriendError(friendLoadError instanceof Error ? friendLoadError.message : 'Could not load friend requests.')
    } finally {
      setFriendLoading(false)
    }
  }, [apiBaseUrl, authFetch])

  useEffect(() => {
    void loadChildren()
  }, [loadChildren])

  useEffect(() => {
    if (selectedId) {
      if (activeView === 'chats') {
        void loadQueue(selectedId)
      }
    } else {
      setThreads([])
      setSelectedThreadId(null)
    }
  }, [activeView, loadQueue, selectedId])

  useEffect(() => {
    if (activeView === 'friends') {
      void loadFriendRequests()
    }
  }, [activeView, loadFriendRequests])

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
            message.id === payload.id ? { ...message, ...payload } : message
          ),
        }))
      )
    } catch (decisionError) {
      setError(decisionError instanceof Error ? decisionError.message : 'Could not update moderation status.')
    }
  }

  const handleFriendDecision = async (requestId: string, action: 'accept' | 'reject') => {
    setFriendError(null)
    try {
      const response = await authFetch(`${apiBaseUrl}/api/friends/requests/${requestId}/${action}`, {
        method: 'POST',
      })
      if (!response.ok) {
        const message = await response.text()
        throw new Error(message || 'Could not update friend request.')
      }
      setFriendRequests((prev) => prev.filter((request) => request.id !== requestId))
      await loadFriendRequests()
    } catch (friendDecisionError) {
      setFriendError(
        friendDecisionError instanceof Error ? friendDecisionError.message : 'Could not update friend request.'
      )
    }
  }

  const renderStatusBadge = (message: ModerationMessage) => {
    if (!message.label) {
      return <span className="llm-badge neutral">LLM: n/a</span>
    }
    const badgeClass = message.label.toLowerCase() === 'safe' ? 'approved' : 'rejected'
    const score = message.score ? ` (${(message.score * 100).toFixed(1)}%)` : ''
    return (
      <span className={`llm-badge ${badgeClass}`}>
        LLM: {message.label}
        {score}
      </span>
    )
  }

  const selectedThread = threads.find((thread) => thread.threadId === selectedThreadId) ?? null
  const friendRequestsForChild = selectedId
    ? friendRequests.filter((request) => request.addresseeAccountId === selectedId)
    : []
  const isFriendsView = activeView === 'friends'

  return (
    <section className="panel-card">
      <header className="panel-header">
        <div>
          <p className="section-label">Moderation</p>
          <h2>{isFriendsView ? 'Friend Requests' : 'Moderate Messages'}</h2>
          <div className={`switcher ${isFriendsView ? 'child' : ''}`}>
            <button
              className={!isFriendsView ? 'active' : ''}
              type="button"
              onClick={() => setActiveView('chats')}
            >
              Chats
            </button>
            <button
              className={isFriendsView ? 'active' : ''}
              type="button"
              onClick={() => setActiveView('friends')}
            >
              Friends
            </button>
            <span className="switch-pill" aria-hidden="true" />
          </div>
        </div>
        <button
          className="ghost"
          type="button"
          onClick={() => {
            if (isFriendsView) {
              void loadFriendRequests()
            } else {
              void (selectedId ? loadQueue(selectedId) : loadChildren())
            }
          }}
          disabled={loading || friendLoading}
        >
          Refresh
        </button>
      </header>

      {isFriendsView ? (friendError ? <p className="form-error">{friendError}</p> : null) : null}
      {isFriendsView ? (friendLoading ? <p className="status">Loading friend requests...</p> : null) : null}
      {!isFriendsView ? (error ? <p className="form-error">{error}</p> : null) : null}
      {!isFriendsView ? (loading ? <p className="status">Loading moderation queue...</p> : null) : null}

      <div className="panel-split">
        <div className="panel-sidebar">
          <p className="panel-meta">Choose child</p>
          {children.length === 0 && !loading ? <p className="status subtle">No children yet.</p> : null}
          {[...children]
            .sort((a, b) => a.displayName.localeCompare(b.displayName))
            .map((child) => (
            <button
              key={child.id}
              type="button"
              className={`panel-item ${child.id === selectedId ? 'active' : ''}`}
              onClick={() => setSelectedId(child.id)}
            >
              {child.displayName}
            </button>
          ))}
          {!isFriendsView ? <p className="panel-meta">Choose thread</p> : null}
          {!isFriendsView && threads.length === 0 && !loading ? <p className="status subtle">No threads yet.</p> : null}
          {!isFriendsView
            ? [...threads]
                .sort((a, b) => a.title.localeCompare(b.title))
                .map((thread) => (
                <button
                  key={thread.threadId}
                  type="button"
                  className={`panel-item ${thread.threadId === selectedThreadId ? 'active' : ''}`}
                  onClick={() => setSelectedThreadId(thread.threadId)}
                >
                  {thread.title}
                </button>
              ))
            : null}
        </div>
        <div className="panel-content">
          {isFriendsView ? (
            <>
              {!selectedId && !friendLoading ? <p className="status subtle">Select a child to review.</p> : null}
              {selectedId && friendRequestsForChild.length === 0 && !friendLoading ? (
                <p className="status subtle">No friend requests yet.</p>
              ) : null}
              {friendRequestsForChild.map((request) => (
                <div className="child-card" key={request.id}>
                  <div className="child-info">
                    {request.requesterAvatarUrl ? (
                      <img className="avatar" src={request.requesterAvatarUrl} alt="" />
                    ) : (
                      <div className="avatar avatar-fallback">
                        {request.requesterDisplayName.slice(0, 2).toUpperCase()}
                      </div>
                    )}
                    <div>
                      <strong>{request.requesterDisplayName}</strong>
                      <p className="panel-meta">
                        For {request.addresseeDisplayName} - {new Date(request.requestedAt).toLocaleString()}
                      </p>
                    </div>
                  </div>
                  <div className="child-actions">
                    <button
                      className="icon-button approve"
                      type="button"
                      title="Approve"
                      onClick={() => handleFriendDecision(request.id, 'accept')}
                    >
                      ✓
                    </button>
                    <button
                      className="icon-button reject"
                      type="button"
                      title="Reject"
                      onClick={() => handleFriendDecision(request.id, 'reject')}
                    >
                      ×
                    </button>
                  </div>
                </div>
              ))}
            </>
          ) : (
            <>
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
                              {isOutgoing ? (
                                <span className="llm-badge neutral">Outgoing</span>
                              ) : (
                                renderStatusBadge(message)
                              )}
                              <span>{new Date(message.createdAt).toLocaleString()}</span>
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
            </>
          )}
        </div>
      </div>
    </section>
  )
}

export default ModerationQueuePanel
