import { type FormEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type {
  FriendListItem,
  FriendRequestItem,
  FriendSearchResponse,
  FriendSearchResult,
} from '../../entities/friendship'
import type { AuthFetch } from '../../shared/apiClient'

type FriendsListProps = {
  apiBaseUrl: string
  authFetch: AuthFetch
  currentAccountId?: string | null
  onSelectFriend: (accountId: string) => void
}

type InviteState = 'idle' | 'sending' | 'sent'

function FriendsList({ apiBaseUrl, authFetch, currentAccountId, onSelectFriend }: FriendsListProps) {
  const [friends, setFriends] = useState<FriendListItem[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [friendRequests, setFriendRequests] = useState<FriendRequestItem[]>([])
  const [friendRequestsLoading, setFriendRequestsLoading] = useState(false)
  const [friendRequestsError, setFriendRequestsError] = useState<string | null>(null)

  const [searchOpen, setSearchOpen] = useState(false)
  const [searchEmail, setSearchEmail] = useState('')
  const [searchResults, setSearchResults] = useState<FriendSearchResult[]>([])
  const [searchLoading, setSearchLoading] = useState(false)
  const [searchError, setSearchError] = useState<string | null>(null)
  const [inviteState, setInviteState] = useState<Record<string, InviteState>>({})
  const [visibleCount, setVisibleCount] = useState(12)
  const loadMoreRef = useRef<HTMLDivElement | null>(null)
  const listContainerRef = useRef<HTMLDivElement | null>(null)

  const loadFriends = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await authFetch(`${apiBaseUrl}/api/friends`)
      if (!response.ok) {
        throw new Error('Could not load friends.')
      }
      const payload = (await response.json()) as FriendListItem[]
      setFriends(payload)
      setVisibleCount(12)
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Could not load friends.')
    } finally {
      setLoading(false)
    }
  }, [apiBaseUrl, authFetch])

  const loadFriendRequests = useCallback(async () => {
    if (!currentAccountId) {
      setFriendRequests([])
      return
    }
    setFriendRequestsLoading(true)
    setFriendRequestsError(null)
    try {
      const response = await authFetch(`${apiBaseUrl}/api/friends/requests/pending`)
      if (!response.ok) {
        throw new Error('Could not load friend requests.')
      }
      const payload = (await response.json()) as FriendRequestItem[]
      setFriendRequests(payload)
    } catch (requestError) {
      setFriendRequestsError(requestError instanceof Error ? requestError.message : 'Could not load friend requests.')
    } finally {
      setFriendRequestsLoading(false)
    }
  }, [apiBaseUrl, authFetch, currentAccountId])

  useEffect(() => {
    void loadFriends()
  }, [loadFriends])

  useEffect(() => {
    void loadFriendRequests()
  }, [loadFriendRequests])

  useEffect(() => {
    if (!loadMoreRef.current) {
      return
    }
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          setVisibleCount((prev) => Math.min(prev + 12, friends.length))
        }
      },
      { root: listContainerRef.current, rootMargin: '120px' }
    )
    observer.observe(loadMoreRef.current)
    return () => observer.disconnect()
  }, [friends.length])

  const handleSearchSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const trimmed = searchEmail.trim()
    if (!trimmed) {
      setSearchError('Enter an email address to search.')
      return
    }
    setSearchLoading(true)
    setSearchError(null)
    setSearchResults([])
    try {
      const response = await authFetch(
        `${apiBaseUrl}/api/friends/search?email=${encodeURIComponent(trimmed)}`
      )
      if (!response.ok) {
        throw new Error('Could not search by email.')
      }
      const payload = (await response.json()) as FriendSearchResponse
      setSearchResults(payload.results ?? [])
    } catch (searchFailure) {
      setSearchError(searchFailure instanceof Error ? searchFailure.message : 'Could not search by email.')
    } finally {
      setSearchLoading(false)
    }
  }

  const handleInvite = async (accountId: string) => {
    setInviteState((prev) => ({ ...prev, [accountId]: 'sending' }))
    try {
      const response = await authFetch(`${apiBaseUrl}/api/friends/requests`, {
        method: 'POST',
        body: JSON.stringify({ addresseeAccountId: accountId }),
      })
      if (!response.ok) {
        throw new Error('Could not send invite.')
      }
      setInviteState((prev) => ({ ...prev, [accountId]: 'sent' }))
    } catch (inviteError) {
      setInviteState((prev) => ({ ...prev, [accountId]: 'idle' }))
      setSearchError(inviteError instanceof Error ? inviteError.message : 'Could not send invite.')
    }
  }

  const handleFriendRequestDecision = async (requestId: string, action: 'accept' | 'reject') => {
    try {
      const response = await authFetch(`${apiBaseUrl}/api/friends/requests/${requestId}/${action}`, {
        method: 'POST',
      })
      if (!response.ok) {
        const message = await response.text()
        throw new Error(message || 'Could not update friend request.')
      }
      setFriendRequests((prev) => prev.filter((request) => request.id !== requestId))
      if (action === 'accept') {
        void loadFriends()
      }
    } catch (requestError) {
      setFriendRequestsError(
        requestError instanceof Error ? requestError.message : 'Could not update friend request.'
      )
    }
  }

  const closeSearch = () => {
    setSearchOpen(false)
    setSearchEmail('')
    setSearchResults([])
    setSearchError(null)
    setInviteState({})
  }

  const friendsEmptyState = useMemo(() => {
    if (loading) {
      return 'Loading friends...'
    }
    if (error) {
      return error
    }
    return 'No friends yet. Invite someone to start chatting.'
  }, [error, loading])

  const formatAccountType = (type: FriendSearchResult['accountType']) =>
    type === 'child' ? 'Child' : 'Parent'

  const fallbackTitle = (title: string) => title.slice(0, 2).toUpperCase()
  const visibleFriends = friends.slice(0, visibleCount)
  const pendingRequests = currentAccountId
    ? friendRequests.filter((request) => request.addresseeAccountId === currentAccountId)
    : []

  return (
    <section className="thread-panel friends-panel">
      <button className="primary" type="button" onClick={() => setSearchOpen(true)}>
        Find a friend
      </button>

      <div className="panel-card">
        {error ? <p className="form-error">{error}</p> : null}
        {loading ? <p className="status">Loading friends...</p> : null}
        <div className="thread-list" ref={listContainerRef}>
          {friendRequestsError ? <p className="form-error">{friendRequestsError}</p> : null}
          {friendRequestsLoading ? <p className="status">Loading friend requests...</p> : null}
          {!friendRequestsLoading && pendingRequests.length > 0 ? (
            <div className="friend-requests">
              <p className="status">Pending friend requests</p>
              {pendingRequests.map((request) => (
                <div className="friend-request-card" key={request.id}>
                  <div className="thread-avatar">
                    {request.requesterAvatarUrl ? (
                      <img className="avatar" src={request.requesterAvatarUrl} alt="" />
                    ) : (
                      <div className="avatar avatar-fallback">
                        {request.requesterDisplayName.slice(0, 2).toUpperCase()}
                      </div>
                    )}
                  </div>
                  <div className="thread-body">
                    <h3>{request.requesterDisplayName}</h3>
                    <p className="thread-preview">
                      Requested {new Date(request.requestedAt).toLocaleString()}
                    </p>
                  </div>
                  <div className="thread-meta">
                    <div className="friend-request-actions">
                      <button
                        className="icon-button approve"
                        type="button"
                        onClick={() => handleFriendRequestDecision(request.id, 'accept')}
                      >
                        ✓
                      </button>
                      <button
                        className="icon-button reject"
                        type="button"
                        onClick={() => handleFriendRequestDecision(request.id, 'reject')}
                      >
                        ×
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : null}
          {!loading && !error && friends.length === 0 ? <p className="status">{friendsEmptyState}</p> : null}
          {visibleFriends.map((friend) => (
            <button
              className="thread-card"
              key={friend.accountId}
              type="button"
              onClick={() => onSelectFriend(friend.accountId)}
            >
              <div className="thread-avatar">
                {friend.avatarUrl ? (
                  <img className="avatar" src={friend.avatarUrl} alt="" />
                ) : (
                  <div className="avatar avatar-fallback">{fallbackTitle(friend.displayName)}</div>
                )}
              </div>
              <div className="thread-body">
                <h3>{friend.displayName}</h3>
                <p className="thread-preview">Tap to start chatting.</p>
              </div>
              <div className="thread-meta">
                <span>Friend</span>
              </div>
            </button>
          ))}
          <div ref={loadMoreRef} />
        </div>
      </div>

      {searchOpen ? (
        <div className="modal-overlay" role="dialog" aria-modal="true">
          <div className="modal-card">
            <header className="panel-header">
              <div>
                <h3>Find a friend</h3>
                <p className="panel-meta">Find parents and their children by email.</p>
              </div>
              <button className="ghost" type="button" onClick={closeSearch}>
                Close
              </button>
            </header>

            <form className="panel-form" onSubmit={handleSearchSubmit}>
              <label className="field" htmlFor="friend-email">
                User email
                <input
                  id="friend-email"
                  type="email"
                  value={searchEmail}
                  onChange={(event) => setSearchEmail(event.target.value)}
                  placeholder="parent@example.com"
                />
              </label>
              <div className="modal-actions">
                <button className="ghost" type="button" onClick={closeSearch}>
                  Cancel
                </button>
                <button className="primary" type="submit" disabled={searchLoading}>
                  {searchLoading ? 'Searching...' : 'Search'}
                </button>
              </div>
            </form>

            {searchError ? <p className="form-error">{searchError}</p> : null}
            {searchLoading ? <p className="status">Searching...</p> : null}

            {!searchLoading && searchResults.length === 0 && !searchError ? (
              <p className="status subtle">No matches yet. Try another email.</p>
            ) : null}

            <div className="panel-list">
              {searchResults.map((result) => {
                const inviteStatus = inviteState[result.accountId] ?? 'idle'
                const isSending = inviteStatus === 'sending'
                const isSent = inviteStatus === 'sent'
                const isPending = result.friendshipStatus === 'PENDING'
                const isDisabled = isSending || isSent || isPending
                const label = isSent
                  ? 'Invited'
                  : isSending
                    ? 'Sending...'
                    : isPending
                      ? 'Pending'
                      : 'Invite'
                return (
                  <div className="child-card" key={result.accountId}>
                    <div className="child-info">
                      {result.avatarUrl ? (
                        <img className="avatar" src={result.avatarUrl} alt="" />
                      ) : (
                        <div className="avatar avatar-fallback">{fallbackTitle(result.displayName)}</div>
                      )}
                      <div>
                        <strong>{result.displayName}</strong>
                        <p className="panel-meta">{formatAccountType(result.accountType)}</p>
                      </div>
                    </div>
                    <div className="child-actions">
                      <button
                        className="primary"
                        type="button"
                        onClick={() => handleInvite(result.accountId)}
                        disabled={isDisabled}
                      >
                        {label}
                      </button>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}

export default FriendsList
