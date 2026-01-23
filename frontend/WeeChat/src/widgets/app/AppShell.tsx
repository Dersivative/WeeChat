import { Client } from '@stomp/stompjs'
import { type FormEvent, type TouchEvent, useEffect, useMemo, useRef, useState } from 'react'
import './app-shell.css'
import { AuthPanel, WelcomeHeader } from '../auth'
import { ChatHeader } from '../chat'
import { AccountMenu } from '../menu'
import { ThreadList } from '../threads'
import type { AccountType, AuthState, LoginResponse } from '../../entities/account'
import type { ThreadListItem, ThreadListResponse } from '../../entities/thread'

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
  const [menuOpen, setMenuOpen] = useState(false)

  const [threads, setThreads] = useState<ThreadListItem[]>([])
  const [threadsLoading, setThreadsLoading] = useState(false)
  const [threadsError, setThreadsError] = useState<string | null>(null)
  const stompClientRef = useRef<Client | null>(null)
  const touchStartRef = useRef<{ x: number; y: number } | null>(null)

  const wsUrl = useMemo(() => {
    const base = apiBaseUrl || window.location.origin
    return `${base.replace(/^http/, 'ws')}/ws`
  }, [apiBaseUrl])

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
    }
  }, [auth])

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

  const handleLogout = () => {
    setAuth(null)
    setMenuOpen(false)
    if (stompClientRef.current) {
      stompClientRef.current.deactivate()
      stompClientRef.current = null
    }
    setThreads([])
    setFormState({ login: '', password: '' })
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
            onFormChange={(field: 'login' | 'password', value: string) =>
              setFormState((prev) => ({ ...prev, [field]: value }))
            }
            onLoginSubmit={handleLoginSubmit}
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
            onMenuOpen={() => setMenuOpen(true)}
            onLogout={handleLogout}
          />

          <AccountMenu
            open={menuOpen}
            accountType={auth.accountType}
            login={auth.profile.login}
            onClose={() => setMenuOpen(false)}
            onLogout={handleLogout}
          />

          <ThreadList
            threads={threads}
            loading={threadsLoading}
            error={threadsError}
          />
        </main>
      )}
    </div>
  )
}

export default AppShell
