import { type FormEvent, useEffect, useRef, useState } from 'react'
import type { AuthState } from '../../entities/account'
import type { MessageItem, MessageListResponse } from '../../entities/message'
import type { ThreadListItem } from '../../entities/thread'
import type { AuthFetch } from '../../shared/apiClient'

type ThreadViewProps = {
  thread: ThreadListItem | null
  auth: AuthState
  apiBaseUrl: string
  authFetch: AuthFetch
  incomingMessage?: MessageItem | null
  recipientAccountId?: string | null
  onThreadCreated?: (threadId: string) => void
}

const PAGE_SIZE = 10
const MESSAGE_UNAVAILABLE = 'Message unavailable'

function ThreadView({
  thread,
  auth,
  apiBaseUrl,
  authFetch,
  incomingMessage = null,
  recipientAccountId = null,
  onThreadCreated,
}: ThreadViewProps) {
  const [messages, setMessages] = useState<MessageItem[]>([])
  const [loading, setLoading] = useState(false)
  const [loadingOlder, setLoadingOlder] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [hasMore, setHasMore] = useState(true)
  const [nextBefore, setNextBefore] = useState<string | null>(null)
  const [draft, setDraft] = useState('')
  const [sending, setSending] = useState(false)
  const [sendError, setSendError] = useState<string | null>(null)
  const listRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    setMessages([])
    setError(null)
    setHasMore(true)
    setNextBefore(null)
    setDraft('')
    setSendError(null)
    if (!thread) {
      return
    }
    void loadMessages({ initial: true })
  }, [thread?.threadId])

  useEffect(() => {
    if (!incomingMessage || !thread) {
      return
    }
    if (incomingMessage.threadId !== thread.threadId) {
      return
    }
    setMessages((prev) => {
      if (prev.some((message) => message.id === incomingMessage.id)) {
        return prev
      }
      return [...prev, incomingMessage]
    })
    requestAnimationFrame(() => {
      if (listRef.current) {
        listRef.current.scrollTop = listRef.current.scrollHeight
      }
    })
  }, [incomingMessage, thread])

  const loadMessages = async ({ initial = false } = {}) => {
    if (!thread) {
      return
    }
    if (initial) {
      setLoading(true)
    } else {
      setLoadingOlder(true)
    }
    setError(null)

    const beforeQuery = initial ? '' : nextBefore ? `&before=${encodeURIComponent(nextBefore)}` : ''
    try {
      const response = await authFetch(
        `${apiBaseUrl}/api/threads/${thread.threadId}/messages?limit=${PAGE_SIZE}${beforeQuery}`,
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      )
      if (!response.ok) {
        throw new Error('Could not load messages.')
      }
      const payload = (await response.json()) as MessageListResponse
      if (initial) {
        setMessages(payload.messages)
        requestAnimationFrame(() => {
          if (listRef.current) {
            listRef.current.scrollTop = listRef.current.scrollHeight
          }
        })
      } else {
        const list = listRef.current
        const previousScrollHeight = list?.scrollHeight ?? 0
        const previousScrollTop = list?.scrollTop ?? 0
        setMessages((prev) => [...payload.messages, ...prev])
        requestAnimationFrame(() => {
          if (listRef.current) {
            const newScrollHeight = listRef.current.scrollHeight
            listRef.current.scrollTop = newScrollHeight - previousScrollHeight + previousScrollTop
          }
        })
      }
      setHasMore(payload.hasMore)
      setNextBefore(payload.nextBefore ?? null)
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Could not load messages.')
    } finally {
      if (initial) {
        setLoading(false)
      } else {
        setLoadingOlder(false)
      }
    }
  }

  const handleScroll = () => {
    if (!thread || loadingOlder || loading || !hasMore) {
      return
    }
    const list = listRef.current
    if (!list) {
      return
    }
    if (list.scrollTop < 120) {
      void loadMessages()
    }
  }

  const handleSend = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if ((!thread && !recipientAccountId) || sending) {
      return
    }
    const trimmed = draft.trim()
    if (!trimmed) {
      return
    }
    setSending(true)
    setSendError(null)
    try {
      const response = thread
        ? await authFetch(`${apiBaseUrl}/api/threads/${thread.threadId}/messages`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({ text: trimmed }),
          })
        : await authFetch(`${apiBaseUrl}/api/threads/direct/messages`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({ recipientAccountId, text: trimmed }),
          })
      if (!response.ok) {
        throw new Error('Could not send message.')
      }
      const payload = (await response.json()) as MessageItem
      if (!thread && payload.threadId && onThreadCreated) {
        onThreadCreated(payload.threadId)
      }
      setMessages((prev) => {
        if (prev.some((message) => message.id === payload.id)) {
          return prev
        }
        return [...prev, payload]
      })
      setDraft('')
      requestAnimationFrame(() => {
        if (listRef.current) {
          listRef.current.scrollTop = listRef.current.scrollHeight
        }
      })
    } catch (sendErrorValue) {
      setSendError(sendErrorValue instanceof Error ? sendErrorValue.message : 'Could not send message.')
    } finally {
      setSending(false)
    }
  }

  if (!thread && !recipientAccountId) {
    return (
      <section className="thread-view empty">
        <p className="status">Select a chat to see messages.</p>
      </section>
    )
  }

  if (!thread && recipientAccountId) {
    return (
      <section className="thread-view">
        <header className="thread-view-header">
          <div>
            <p className="section-label">Conversation</p>
            <h2>New chat</h2>
          </div>
        </header>
        <div className="message-list" ref={listRef}>
          <p className="status subtle">Start the conversation.</p>
        </div>
        <form className="message-composer" onSubmit={handleSend}>
          <input
            type="text"
            placeholder="Write a message..."
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
          />
          <button className="primary" type="submit" disabled={sending || !draft.trim()}>
            {sending ? 'Sending...' : 'Send'}
          </button>
        </form>
        {sendError ? <p className="form-error">{sendError}</p> : null}
      </section>
    )
  }

  const activeThread = thread as ThreadListItem

  return (
    <section className="thread-view">
      <header className="thread-view-header">
        <div>
          <p className="section-label">Conversation</p>
          <h2>{activeThread.title}</h2>
        </div>
        <span className="thread-count">{messages.length} messages</span>
      </header>

      {loading ? <p className="status">Loading messages...</p> : null}
      {error ? <p className="form-error">{error}</p> : null}
      <div className="message-list" ref={listRef} onScroll={handleScroll}>
        {loadingOlder ? <p className="status subtle">Loading older messages...</p> : null}
        {messages.map((message) => {
          const isOwn = message.senderAccountId === auth.profile.accountId
          return (
            <article key={message.id} className={`message-row ${isOwn ? 'own' : ''}`}>
              <div className="message-bubble">
                <p className="message-text">{message.text || MESSAGE_UNAVAILABLE}</p>
                <span className="message-meta">{new Date(message.createdAt).toLocaleTimeString()}</span>
              </div>
            </article>
          )
        })}
        {!loading && messages.length === 0 ? (
          <p className="status subtle">No messages yet. Start the conversation.</p>
        ) : null}
      </div>

      <form className="message-composer" onSubmit={handleSend}>
        <input
          type="text"
          placeholder="Write a message..."
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
        />
        <button className="primary" type="submit" disabled={sending || !draft.trim()}>
          {sending ? 'Sending...' : 'Send'}
        </button>
      </form>
      {sendError ? <p className="form-error">{sendError}</p> : null}
    </section>
  )
}

export default ThreadView
