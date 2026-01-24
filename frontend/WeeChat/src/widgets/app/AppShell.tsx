import { Client } from '@stomp/stompjs'
import { type FormEvent, type TouchEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import './app-shell.css'
import { AuthPanel, WelcomeHeader } from '../auth'
import { ChatHeader, ChatViewSwitch } from '../chat'
import { FriendsList } from '../friends'
import { AccountMenu, type AccountPanel } from '../menu'
import { ChildrenManager, ModerationQueuePanel, ModerationSettingsPanel } from '../children'
import { ThreadList, ThreadView } from '../threads'
import type { AccountType, AuthState, ChildLoginResponse, LoginResponse } from '../../entities/account'
import type { MessageItem } from '../../entities/message'
import type { ThreadListItem, ThreadListResponse } from '../../entities/thread'
import type { AuthFetch } from '../../shared/apiClient'

const AUTH_STORAGE_KEY = 'weechat.auth'
const TOKEN_REFRESH_BUFFER_SECONDS = 60
const TOKEN_REFRESH_INTERVAL_MS = 30_000

const decodeJwtPayload = (token: string): { exp?: number } | null => {
  try {
    const payload = token.split('.')[1]
    if (!payload) {
      return null
    }
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
    const decoded = window.atob(padded)
    return JSON.parse(decoded) as { exp?: number }
  } catch {
    return null
  }
}

const isTokenExpiringSoon = (token: string, bufferSeconds: number) => {
  const payload = decodeJwtPayload(token)
  if (!payload?.exp) {
    return false
  }
  const nowSeconds = Date.now() / 1000
  return nowSeconds >= payload.exp - bufferSeconds
}

function AppShell() {
  const apiBaseUrl =
    typeof import.meta.env.VITE_API_BASE_URL === 'string'
      ? import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '')
      : ''
  const [formState, setFormState] = useState({ login: '', password: '' })
  const [loginRole, setLoginRole] = useState<AccountType>('user')
  const [auth, setAuth] = useState<AuthState | null>(null)
  const [loginError, setLoginError] = useState<string | null>(null)
  const [loginLoading, setLoginLoading] = useState(false)
  const [childLoginError, setChildLoginError] = useState<string | null>(null)
  const [childLoginLoading, setChildLoginLoading] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const [activePanel, setActivePanel] = useState<AccountPanel>('chats')
  const [chatView, setChatView] = useState<'chats' | 'friends'>('chats')

  const [threads, setThreads] = useState<ThreadListItem[]>([])
  const [threadsLoading, setThreadsLoading] = useState(false)
  const [threadsError, setThreadsError] = useState<string | null>(null)
  const [selectedThreadId, setSelectedThreadId] = useState<string | null>(null)
  const [pendingRecipientId, setPendingRecipientId] = useState<string | null>(null)
  const [incomingMessage, setIncomingMessage] = useState<MessageItem | null>(null)
  const stompClientRef = useRef<Client | null>(null)
  const touchStartRef = useRef<{ x: number; y: number } | null>(null)
  const refreshPromiseRef = useRef<Promise<AuthState | null> | null>(null)

  const wsUrl = useMemo(() => {
    const base = apiBaseUrl || window.location.origin
    return `${base.replace(/^http/, 'ws')}/ws`
  }, [apiBaseUrl])

  useEffect(() => {
    const storedAuth = window.localStorage.getItem(AUTH_STORAGE_KEY)
    if (!storedAuth) {
      return
    }
    try {
      const parsedAuth = JSON.parse(storedAuth) as AuthState
      if (parsedAuth?.accessToken && parsedAuth?.refreshToken && parsedAuth?.profile) {
        setAuth(parsedAuth)
        setLoginRole(parsedAuth.accountType)
      } else {
        window.localStorage.removeItem(AUTH_STORAGE_KEY)
      }
    } catch {
      window.localStorage.removeItem(AUTH_STORAGE_KEY)
    }
  }, [])

  useEffect(() => {
    if (auth) {
      window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth))
      return
    }
    window.localStorage.removeItem(AUTH_STORAGE_KEY)
  }, [auth])

  const refreshTokens = useCallback(async () => {
    if (!auth?.refreshToken) {
      return auth
    }
    if (refreshPromiseRef.current) {
      return refreshPromiseRef.current
    }
    refreshPromiseRef.current = (async () => {
      const response = await fetch(`${apiBaseUrl}/api/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ refreshToken: auth.refreshToken }),
      })
      if (!response.ok) {
        throw new Error('Could not refresh session.')
      }
      const payload = (await response.json()) as { accessToken: string; refreshToken: string }
      const nextAuth = {
        ...auth,
        accessToken: payload.accessToken,
        refreshToken: payload.refreshToken,
        profile: {
          ...auth.profile,
          accessToken: payload.accessToken,
          refreshToken: payload.refreshToken,
        },
      }
      setAuth(nextAuth)
      return nextAuth
    })()
      .catch((error) => {
        setAuth(null)
        throw error
      })
      .finally(() => {
        refreshPromiseRef.current = null
      })
    return refreshPromiseRef.current
  }, [apiBaseUrl, auth])

  const authFetch = useCallback<AuthFetch>(
    async (url, init) => {
      if (!auth) {
        throw new Error('Not authenticated.')
      }
      let activeAuth = auth
      if (auth.refreshToken && isTokenExpiringSoon(auth.accessToken, TOKEN_REFRESH_BUFFER_SECONDS)) {
        const refreshed = await refreshTokens()
        if (refreshed) {
          activeAuth = refreshed
        }
      }
      const headers = new Headers(init?.headers)
      headers.set('Authorization', `Bearer ${activeAuth.accessToken}`)
      if (init?.body && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json')
      }
      const response = await fetch(url, { ...init, headers })
      if (response.status === 401 && auth.refreshToken) {
        const refreshed = await refreshTokens()
        if (refreshed) {
          const retryHeaders = new Headers(init?.headers)
          retryHeaders.set('Authorization', `Bearer ${refreshed.accessToken}`)
          if (init?.body && !retryHeaders.has('Content-Type')) {
            retryHeaders.set('Content-Type', 'application/json')
          }
          return fetch(url, { ...init, headers: retryHeaders })
        }
      }
      return response
    },
    [auth, refreshTokens]
  )

  useEffect(() => {
    if (!auth?.refreshToken) {
      return
    }
    const interval = window.setInterval(() => {
      if (auth.refreshToken && isTokenExpiringSoon(auth.accessToken, TOKEN_REFRESH_BUFFER_SECONDS)) {
        void refreshTokens()
      }
    }, TOKEN_REFRESH_INTERVAL_MS)
    return () => window.clearInterval(interval)
  }, [auth, refreshTokens])

  useEffect(() => {
    if (!auth) {
      return
    }

    const client = new Client({
      brokerURL: wsUrl,
      connectHeaders: {
        Authorization: `Bearer ${auth.accessToken}`,
      },
      reconnectDelay: 4000,
      onConnect: () => {
        setThreadsLoading(true)
        setThreadsError(null)
        client.subscribe('/user/queue/threads', (message) => {
          try {
            const payload = JSON.parse(message.body) as ThreadListResponse
            setThreads(payload.threads ?? [])
          } catch (error) {
            setThreadsError('Could not parse chat list.')
          } finally {
            setThreadsLoading(false)
          }
        })
        client.subscribe('/user/queue/messages', (message) => {
          try {
            const payload = JSON.parse(message.body) as MessageItem
            setIncomingMessage(payload)
            setThreads((prev) => {
              const index = prev.findIndex((thread) => thread.threadId === payload.threadId)
              if (index === -1) {
                return prev
              }
              const updated = {
                ...prev[index],
                lastMessageText: payload.text,
                lastMessageAt: payload.createdAt,
                unread: payload.senderAccountId !== auth.profile.accountId,
              }
              const next = prev.filter((_, itemIndex) => itemIndex !== index)
              return [updated, ...next]
            })
          } catch {
            // ignore malformed message payloads
          }
        })
        client.publish({ destination: '/app/threads/list', body: '' })
      },
      onStompError: (frame) => {
        setThreadsError(frame.headers.message || 'Could not load chats.')
        setThreadsLoading(false)
      },
      onWebSocketError: () => {
        setThreadsError('Could not connect to chat service.')
        setThreadsLoading(false)
      },
    })

    stompClientRef.current = client
    client.activate()

    return () => {
      client.deactivate()
      stompClientRef.current = null
    }
  }, [auth, wsUrl])

  useEffect(() => {
    if (!menuOpen) {
      return
    }
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setMenuOpen(false)
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => {
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [menuOpen])

  useEffect(() => {
    if (!auth) {
      setMenuOpen(false)
      setActivePanel('chats')
      setChatView('chats')
    }
  }, [auth])

  useEffect(() => {
    if (!auth) {
      return
    }
    if (auth.accountType !== 'user') {
      setActivePanel('chats')
    }
  }, [auth])

  useEffect(() => {
    if (activePanel !== 'chats') {
      setChatView('chats')
    }
  }, [activePanel])

  useEffect(() => {
    if (!auth) {
      setSelectedThreadId(null)
      return
    }
    if (pendingRecipientId) {
      return
    }
    if (threads.length === 0) {
      setSelectedThreadId(null)
      return
    }
    if (!selectedThreadId) {
      setSelectedThreadId(threads[0].threadId)
      return
    }
    if (!threads.some((thread) => thread.threadId === selectedThreadId)) {
      setSelectedThreadId(threads[0].threadId)
    }
  }, [auth, pendingRecipientId, threads, selectedThreadId])

  const handleLoginSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoginError(null)
    setLoginLoading(true)

    try {
      const response = await fetch(`${apiBaseUrl}/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formState),
      })

      if (!response.ok) {
        throw new Error('Invalid login or password.')
      }

      const payload = (await response.json()) as LoginResponse
      setAuth({
        login: formState.login,
        accessToken: payload.accessToken,
        refreshToken: payload.refreshToken,
        accountType: loginRole,
        profile: payload,
      })
    } catch (error) {
      setLoginError(error instanceof Error ? error.message : 'Login failed.')
    } finally {
      setLoginLoading(false)
    }
  }

  const handleChildLogin = async (code: string) => {
    const token = code.trim()
    if (!token) {
      return
    }
    setChildLoginError(null)
    setChildLoginLoading(true)
    try {
      const response = await fetch(`${apiBaseUrl}/api/children/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ token }),
      })
      if (!response.ok) {
        throw new Error('Invalid login code.')
      }
      const payload = (await response.json()) as ChildLoginResponse
      const profile: LoginResponse = {
        accountId: payload.accountId,
        login: payload.displayName,
        twoFactorEnabled: false,
        avatarUrl: payload.avatarUrl,
        accessToken: payload.accessToken,
        refreshToken: '',
      }
      setAuth({
        login: payload.displayName,
        accessToken: payload.accessToken,
        refreshToken: '',
        accountType: 'child',
        profile,
      })
    } catch (error) {
      setChildLoginError(error instanceof Error ? error.message : 'Login failed.')
    } finally {
      setChildLoginLoading(false)
    }
  }

  const handleLogout = () => {
    setAuth(null)
    setMenuOpen(false)
    setSelectedThreadId(null)
    setIncomingMessage(null)
    setActivePanel('chats')
    if (stompClientRef.current) {
      stompClientRef.current.deactivate()
      stompClientRef.current = null
    }
    setThreads([])
    setFormState({ login: '', password: '' })
    setChildLoginError(null)
    setChildLoginLoading(false)
  }

  const handleTouchStart = (event: TouchEvent<HTMLElement>) => {
    if (event.touches.length !== 1) {
      touchStartRef.current = null
      return
    }
    const touch = event.touches[0]
    touchStartRef.current = { x: touch.clientX, y: touch.clientY }
  }

  const handleTouchEnd = (event: TouchEvent<HTMLElement>) => {
    const start = touchStartRef.current
    if (!start || event.changedTouches.length !== 1) {
      touchStartRef.current = null
      return
    }
    const touch = event.changedTouches[0]
    const deltaX = touch.clientX - start.x
    const deltaY = touch.clientY - start.y
    touchStartRef.current = null
    if (deltaX < -70 && Math.abs(deltaY) < 90) {
      setMenuOpen(true)
    }
  }

  const selectedThread = threads.find((thread) => thread.threadId === selectedThreadId) ?? null
  const showChat = activePanel === 'chats' || auth?.accountType !== 'user'

  const refreshThreads = () => {
    if (stompClientRef.current) {
      stompClientRef.current.publish({ destination: '/app/threads/list', body: '' })
    }
  }

  const handleFriendSelect = (accountId: string) => {
    setActivePanel('chats')
    setChatView('chats')
    const match = threads.find((thread) => thread.memberAccountIds?.includes(accountId))
    if (match) {
      setSelectedThreadId(match.threadId)
      setPendingRecipientId(null)
    } else {
      setSelectedThreadId(null)
      setPendingRecipientId(accountId)
    }
  }

  const handleThreadCreated = (threadId: string) => {
    setPendingRecipientId(null)
    setSelectedThreadId(threadId)
    refreshThreads()
  }

  return (
    <div className={`app ${menuOpen ? 'menu-open' : ''}`}>
      {!auth ? (
        <main className="welcome-card">
          <WelcomeHeader />

          <AuthPanel
            formState={formState}
            role={loginRole}
            onRoleChange={setLoginRole}
            loginError={loginError}
            loginLoading={loginLoading}
            childLoginError={childLoginError}
            childLoginLoading={childLoginLoading}
            onFormChange={(field: 'login' | 'password', value: string) =>
              setFormState((prev) => ({ ...prev, [field]: value }))
            }
            onLoginSubmit={handleLoginSubmit}
            onChildLoginSubmit={handleChildLogin}
          />
        </main>
      ) : (
        <main
          className="chat-shell"
          onTouchStart={handleTouchStart}
          onTouchEnd={handleTouchEnd}
        >
          <ChatHeader
            login={auth.profile.login}
            avatarUrl={auth.profile.avatarUrl}
            menuOpen={menuOpen}
            viewMode={chatView}
            onMenuOpen={() => setMenuOpen(true)}
            onLogout={handleLogout}
          />

          <AccountMenu
            open={menuOpen}
            accountType={auth.accountType}
            login={auth.profile.login}
            onClose={() => setMenuOpen(false)}
            onLogout={handleLogout}
            onSelectPanel={(panel) => {
              setActivePanel(panel)
              setMenuOpen(false)
            }}
          />

          {activePanel === 'chats' ? (
            <div className="chat-view-switch">
              <ChatViewSwitch viewMode={chatView} onViewChange={setChatView} />
            </div>
          ) : null}

          {showChat ? (
            <div className={`chat-body ${chatView === 'friends' ? 'friends-body' : ''}`}>
              {chatView === 'chats' ? (
                <>
                  <section className="thread-panel">
                    <ThreadList
                      threads={threads}
                      loading={threadsLoading}
                      error={threadsError}
                      selectedThreadId={selectedThreadId}
                      onSelectThread={setSelectedThreadId}
                    />
                  </section>
                  <ThreadView
                    thread={selectedThread}
                    auth={auth}
                    authFetch={authFetch}
                    apiBaseUrl={apiBaseUrl}
                    incomingMessage={incomingMessage}
                    recipientAccountId={pendingRecipientId}
                    onThreadCreated={handleThreadCreated}
                  />
                </>
              ) : (
                <div className="panel-body">
                  <FriendsList apiBaseUrl={apiBaseUrl} authFetch={authFetch} onSelectFriend={handleFriendSelect} />
                </div>
              )}
            </div>
          ) : (
            <div className="panel-body">
              {activePanel === 'manage-children' ? (
                <ChildrenManager apiBaseUrl={apiBaseUrl} authFetch={authFetch} />
              ) : null}
              {activePanel === 'moderation-settings' ? (
                <ModerationSettingsPanel apiBaseUrl={apiBaseUrl} authFetch={authFetch} />
              ) : null}
              {activePanel === 'moderation-queue' ? (
                <ModerationQueuePanel apiBaseUrl={apiBaseUrl} authFetch={authFetch} />
              ) : null}
            </div>
          )}
        </main>
      )}
    </div>
  )
}

export default AppShell
