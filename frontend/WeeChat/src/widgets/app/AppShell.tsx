import { Client } from '@stomp/stompjs'
import { type FormEvent, type TouchEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
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
import type { AuthFetch, RegisterRequest } from '../../shared/apiClient' 

const VIEW_PARAM = 'view'
type ViewValue = 'register' | 'chats' | 'friends' | 'children' | 'moderation-settings' | 'moderation-queue'

const PATH_TO_VIEW: Record<string, ViewValue> = {
  '/register': 'register',
  '/chats': 'chats',
  '/friends': 'friends',
  '/children': 'children',
  '/moderation/settings': 'moderation-settings',
  '/moderation/queue': 'moderation-queue',
}

function AppShell() {
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const currentView = searchParams.get(VIEW_PARAM)

  const setView = useCallback(
    (view: ViewValue | null) => {
      navigate(view ? `/?${VIEW_PARAM}=${encodeURIComponent(view)}` : '/')
    },
    [navigate]
  )

  const envApiBaseUrl =
    typeof import.meta.env.VITE_API_BASE_URL === 'string' ? import.meta.env.VITE_API_BASE_URL.trim() : ''
  // When env is unset in production build: use same origin (no api. subdomain)
  const fallbackApiBaseUrl = ''

  const isDev = import.meta.env.DEV
  const apiBaseUrl = isDev ? '' : (envApiBaseUrl || fallbackApiBaseUrl).replace(/\/$/, '')

  const [formState, setFormState] = useState({ login: '', password: '', email: '', displayName: '' })
  
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
  const refreshInFlightRef = useRef(false)
  const authRef = useRef<AuthState | null>(null)
  const sessionMarkerKey = 'weechat.session'


  const wsUrl = useMemo(() => {
    const base = apiBaseUrl || window.location.origin
    return `${base.replace(/^http/, 'ws')}/ws`
  }, [apiBaseUrl])

  const clearSessionState = useCallback(() => {
    setAuth(null)
    setMenuOpen(false)
    setSelectedThreadId(null)
    setPendingRecipientId(null)
    setIncomingMessage(null)
    setActivePanel('chats')
    setThreads([])
    if (stompClientRef.current) {
      stompClientRef.current.deactivate()
      stompClientRef.current = null
    }
    window.sessionStorage.removeItem(sessionMarkerKey)
  }, [])


  const refreshUserSession = useCallback(async () => {
    const response = await fetch(`${apiBaseUrl}/api/auth/refresh`, { method: 'POST', credentials: 'include' })
    if (!response.ok) return null
    const payload = (await response.json()) as LoginResponse
    const nextAuth: AuthState = { accountType: 'user', profile: payload }
    setAuth(nextAuth)
    setLoginRole('user')
    window.sessionStorage.setItem(sessionMarkerKey, 'user')
    return nextAuth
  }, [apiBaseUrl])

  const refreshChildSession = useCallback(async () => {
    const response = await fetch(`${apiBaseUrl}/api/children/refresh`, { method: 'POST', credentials: 'include' })
    if (!response.ok) return null
    const payload = (await response.json()) as ChildLoginResponse
    const profile: LoginResponse = { accountId: payload.accountId, login: payload.displayName, twoFactorEnabled: false, avatarUrl: payload.avatarUrl }
    const nextAuth: AuthState = { accountType: 'child', profile }
    setAuth(nextAuth)
    setLoginRole('child')
    window.sessionStorage.setItem(sessionMarkerKey, 'child')
    return nextAuth
  }, [apiBaseUrl])

  const refreshSession = useCallback(async (options?: { preferRole?: AccountType | null }) => {
      if (refreshInFlightRef.current) return null
      refreshInFlightRef.current = true
      try {
        if (options?.preferRole === 'user') { const u = await refreshUserSession(); if(u) return u }
        else if (options?.preferRole === 'child') { const c = await refreshChildSession(); if(c) return c }
        else { const u = await refreshUserSession(); if(u) return u; const c = await refreshChildSession(); if(c) return c }
        if (authRef.current) clearSessionState()
        return null
      } finally { refreshInFlightRef.current = false }
    }, [clearSessionState, refreshChildSession, refreshUserSession])

  useEffect(() => {
    const marker = window.sessionStorage.getItem(sessionMarkerKey)
    if (!marker) return
    void refreshSession({ preferRole: marker === 'child' ? 'child' : 'user' })
  }, [refreshSession])

  useEffect(() => { authRef.current = auth }, [auth])

  // Migrate old path-based URLs to query param (e.g. /register -> /?view=register)
  useEffect(() => {
    const path = location.pathname.replace(/\/+$/, '') || '/'
    if (path !== '/' && PATH_TO_VIEW[path]) {
      navigate(`/?${VIEW_PARAM}=${PATH_TO_VIEW[path]}`, { replace: true })
    }
  }, [location.pathname, navigate])

  useEffect(() => {
    if (currentView === 'register') return
    if (currentView === 'friends') { setActivePanel('chats'); setChatView('friends'); return }
    if (currentView === 'chats') { setActivePanel('chats'); setChatView('chats'); return }
    if (currentView === 'children') { setActivePanel('manage-children'); return }
    if (currentView === 'moderation-settings') { setActivePanel('moderation-settings'); return }
    if (currentView === 'moderation-queue') { setActivePanel('moderation-queue'); return }
    if (!currentView && authRef.current) { setActivePanel('chats'); setChatView('chats') }
  }, [auth, currentView])

  useEffect(() => {
    if (!auth) return
    const intervalId = window.setInterval(() => { void refreshSession({ preferRole: auth.accountType }) }, 4 * 60 * 1000)
    return () => { window.clearInterval(intervalId) }
  }, [auth, refreshSession])

  const authFetch = useCallback<AuthFetch>(async (url, init) => {
      if (!auth) throw new Error('Not authenticated.')
      const headers = new Headers(init?.headers)
      if (init?.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
      const response = await fetch(url, { ...init, headers, credentials: 'include' })
      if (response.status === 401) {
        const refreshed = await refreshSession({ preferRole: auth.accountType })
        if (refreshed) {
           const retryHeaders = new Headers(init?.headers)
           if (init?.body && !retryHeaders.has('Content-Type')) retryHeaders.set('Content-Type', 'application/json')
           return fetch(url, { ...init, headers: retryHeaders, credentials: 'include' })
        }
      }
      return response
    }, [auth, refreshSession])

  useEffect(() => {
    if (!auth) return
    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 4000,
      onConnect: () => {
        setThreadsLoading(true)
        setThreadsError(null)
        client.subscribe('/user/queue/threads', (message) => {
          try {
            const payload = JSON.parse(message.body) as ThreadListResponse
            setThreads(payload.threads ?? [])
          } catch (error) { setThreadsError('Could not parse chat list.') } finally { setThreadsLoading(false) }
        })
        client.subscribe('/user/queue/messages', (message) => {
          try {
            const payload = JSON.parse(message.body) as MessageItem
            setIncomingMessage(payload)
            setThreads((prev) => {
              const index = prev.findIndex((thread) => thread.threadId === payload.threadId)
              if (index === -1) return prev
              const updated = { ...prev[index], lastMessageText: payload.text, lastMessageAt: payload.createdAt, unread: payload.senderAccountId !== auth.profile.accountId }
              const next = prev.filter((_, itemIndex) => itemIndex !== index)
              return [updated, ...next]
            })
          } catch { /* ignore */ }
        })
        client.publish({ destination: '/app/threads/list', body: '' })
      },
      onStompError: (frame) => { setThreadsError(frame.headers.message || 'Could not load chats.'); setThreadsLoading(false) },
      onWebSocketError: () => { setThreadsError('Could not connect to chat service.'); setThreadsLoading(false) },
    })
    stompClientRef.current = client
    client.activate()
    return () => { client.deactivate(); stompClientRef.current = null }
  }, [auth, wsUrl])

  useEffect(() => {
    if (!menuOpen) return
    const handleKeyDown = (event: KeyboardEvent) => { if (event.key === 'Escape') setMenuOpen(false) }
    window.addEventListener('keydown', handleKeyDown)
    return () => { window.removeEventListener('keydown', handleKeyDown) }
  }, [menuOpen])

  useEffect(() => { if (!auth) { setMenuOpen(false); setActivePanel('chats'); setChatView('chats') } }, [auth])
  useEffect(() => { if (auth && auth.accountType !== 'user') setActivePanel('chats') }, [auth])
  useEffect(() => { if (activePanel !== 'chats') setChatView('chats') }, [activePanel])
  useEffect(() => {
    if (!auth) { setSelectedThreadId(null); return }
    if (pendingRecipientId) return
    if (threads.length === 0) { setSelectedThreadId(null); return }
    if (!selectedThreadId) { setSelectedThreadId(threads[0].threadId); return }
    if (!threads.some((thread) => thread.threadId === selectedThreadId)) setSelectedThreadId(threads[0].threadId)
  }, [auth, pendingRecipientId, selectedThreadId, threads])

  const handleLoginSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoginError(null)
    setLoginLoading(true)
    try {
      const response = await fetch(`${apiBaseUrl}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formState),
        credentials: 'include',
      })
      if (!response.ok) throw new Error('Invalid login or password.')
      const payload = (await response.json()) as LoginResponse
      setAuth({ accountType: loginRole, profile: payload })
      window.sessionStorage.setItem(sessionMarkerKey, loginRole)
    } catch (error) {
      setLoginError(error instanceof Error ? error.message : 'Login failed.')
    } finally {
      setLoginLoading(false)
    }
  }

  const handleRegisterSubmit = async (data: RegisterRequest) => {
    setLoginError(null)
    setLoginLoading(true)
    try {
      const response = await fetch(`${apiBaseUrl}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
        credentials: 'include',
      })

      if (!response.ok) {
         const text = await response.text()
         let msg = text
         try {
             const json = JSON.parse(text)
             msg = json.message ?? json.error ?? json.detail ?? msg
         } catch {}
         throw new Error(msg || 'Registration failed')
      }

      await handleLoginSubmit({ preventDefault: () => {} } as any)
      
    } catch (error) {
      setLoginError(error instanceof Error ? error.message : 'Registration failed.')
    } finally {
      setLoginLoading(false)
    }
  }

  const handleChildLogin = async (code: string) => {
    const token = code.trim()
    if (!token) return
    setChildLoginError(null)
    setChildLoginLoading(true)
    try {
      const response = await fetch(`${apiBaseUrl}/api/children/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token }),
        credentials: 'include',
      })
      if (!response.ok) throw new Error('Invalid login code.')
      const payload = (await response.json()) as ChildLoginResponse
      const profile: LoginResponse = { accountId: payload.accountId, login: payload.displayName, twoFactorEnabled: false, avatarUrl: payload.avatarUrl }
      setAuth({ accountType: 'child', profile })
      window.sessionStorage.setItem(sessionMarkerKey, 'child')
    } catch (error) {
      setChildLoginError(error instanceof Error ? error.message : 'Login failed.')
    } finally {
      setChildLoginLoading(false)
    }
  }

  const handleLogout = () => {
    void fetch(`${apiBaseUrl}/api/auth/logout`, { method: 'POST', credentials: 'include' })
    clearSessionState()
    setFormState({ login: '', password: '', email: '', displayName: '' })
    setChildLoginError(null)
    setChildLoginLoading(false)
    setView(null)
  }

  const handleTouchStart = (event: TouchEvent<HTMLElement>) => {
    if (event.touches.length !== 1) { touchStartRef.current = null; return }
    const touch = event.touches[0]
    touchStartRef.current = { x: touch.clientX, y: touch.clientY }
  }

  const handleTouchEnd = (event: TouchEvent<HTMLElement>) => {
    const start = touchStartRef.current
    if (!start || event.changedTouches.length !== 1) { touchStartRef.current = null; return }
    const touch = event.changedTouches[0]
    const deltaX = touch.clientX - start.x
    const deltaY = touch.clientY - start.y
    touchStartRef.current = null
    if (deltaX < -70 && Math.abs(deltaY) < 90) setMenuOpen(true)
  }

  const selectedThread = threads.find((thread) => thread.threadId === selectedThreadId) ?? null
  const showChat = activePanel === 'chats' || auth?.accountType !== 'user'
  const refreshThreads = () => { if (stompClientRef.current) stompClientRef.current.publish({ destination: '/app/threads/list', body: '' }) }

  const handleFriendSelect = (accountId: string) => {
    setActivePanel('chats'); setChatView('chats')
    const match = threads.find((thread) => thread.memberAccountIds?.includes(accountId))
    if (match) { setSelectedThreadId(match.threadId); setPendingRecipientId(null); setView('chats') }
    else { setSelectedThreadId(null); setPendingRecipientId(accountId); setView('chats') }
  }

  const handleThreadCreated = (threadId: string) => { setPendingRecipientId(null); setSelectedThreadId(threadId); refreshThreads(); setView('chats') }

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
            registerPath={currentView === 'register'}
            onNavigateToRegister={() => setView('register')}
            onNavigateToLogin={() => setView(null)}
            onFormChange={(field: string, value: string) =>
              setFormState((prev) => ({ ...prev, [field]: value }))
            }
            onLoginSubmit={handleLoginSubmit}
            onRegisterSubmit={handleRegisterSubmit}
            onChildLoginSubmit={handleChildLogin}
          />
        </main>
      ) : (
        <main className="chat-shell" onTouchStart={handleTouchStart} onTouchEnd={handleTouchEnd}>
          <ChatHeader login={auth.profile.login} avatarUrl={auth.profile.avatarUrl} menuOpen={menuOpen} viewMode={chatView} onMenuOpen={() => setMenuOpen(true)} onLogout={handleLogout} />
          <AccountMenu open={menuOpen} accountType={auth.accountType} login={auth.profile.login} onClose={() => setMenuOpen(false)} onLogout={handleLogout}
            onSelectPanel={(panel) => {
              setActivePanel(panel); setMenuOpen(false)
              if (panel === 'chats') { setChatView('chats'); setView('chats'); return }
              if (panel === 'manage-children') { setView('children'); return }
              if (panel === 'moderation-settings') { setView('moderation-settings'); return }
              if (panel === 'moderation-queue') { setView('moderation-queue') }
            }}
          />
          {activePanel === 'chats' ? (
            <div className="chat-view-switch">
              <ChatViewSwitch viewMode={chatView} onViewChange={(nextView) => { setChatView(nextView); setView(nextView === 'friends' ? 'friends' : 'chats') }} />
            </div>
          ) : null}
          {showChat ? (
            <div className={`chat-body ${chatView === 'friends' ? 'friends-body' : ''}`}>
              {chatView === 'chats' ? (
                <>
                  <section className="thread-panel">
                    <ThreadList threads={threads} loading={threadsLoading} error={threadsError} selectedThreadId={selectedThreadId} onSelectThread={setSelectedThreadId} />
                  </section>
                  <ThreadView thread={selectedThread} auth={auth} authFetch={authFetch} apiBaseUrl={apiBaseUrl} incomingMessage={incomingMessage} recipientAccountId={pendingRecipientId} onThreadCreated={handleThreadCreated} />
                </>
              ) : (
                <div className="panel-body"><FriendsList apiBaseUrl={apiBaseUrl} authFetch={authFetch} currentAccountId={auth.profile.accountId} onSelectFriend={handleFriendSelect} /></div>
              )}
            </div>
          ) : (
            <div className="panel-body">
              {activePanel === 'manage-children' ? <ChildrenManager apiBaseUrl={apiBaseUrl} authFetch={authFetch} /> : null}
              {activePanel === 'moderation-settings' ? <ModerationSettingsPanel apiBaseUrl={apiBaseUrl} authFetch={authFetch} /> : null}
              {activePanel === 'moderation-queue' ? <ModerationQueuePanel apiBaseUrl={apiBaseUrl} authFetch={authFetch} /> : null}
            </div>
          )}
        </main>
      )}
    </div>
  )
}

export default AppShell