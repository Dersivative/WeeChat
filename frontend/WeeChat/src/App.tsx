import { Client } from '@stomp/stompjs'
import { type FormEvent, useEffect, useMemo, useRef, useState } from 'react'
import './App.css'
import AuthPanel from './components/auth/AuthPanel'
import ThreadList from './components/threads/ThreadList'
import type { AuthState, LoginResponse, ThreadListResponse } from './types'

function App() {
  const apiBaseUrl =
    typeof import.meta.env.VITE_API_BASE_URL === 'string'
      ? import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '')
      : ''
  const [formState, setFormState] = useState({ login: '', password: '' })
  const [auth, setAuth] = useState<AuthState | null>(null)
  const [loginError, setLoginError] = useState<string | null>(null)
  const [loginLoading, setLoginLoading] = useState(false)

  const [threads, setThreads] = useState<ThreadListItem[]>([])
  const [threadsLoading, setThreadsLoading] = useState(false)
  const [threadsError, setThreadsError] = useState<string | null>(null)
  const stompClientRef = useRef<Client | null>(null)

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
    if (stompClientRef.current) {
      stompClientRef.current.deactivate()
      stompClientRef.current = null
    }
    setThreads([])
    setFormState({ login: '', password: '' })
  }

  return (
    <div className="app">
      {!auth ? (
        <main className="welcome-card">
          <header className="welcome-header">
            <div className="badge">WeeChat</div>
            <h1>Welcome back!</h1>
            <p className="subtitle">
              Sign in to pick up your family chats where you left off.
            </p>
          </header>

          <AuthPanel
            formState={formState}
            loginError={loginError}
            loginLoading={loginLoading}
            onFormChange={(field, value) =>
              setFormState((prev) => ({ ...prev, [field]: value }))
            }
            onLoginSubmit={handleLoginSubmit}
          />
        </main>
      ) : (
        <main className="chat-shell">
          <header className="chat-header">
            <div className="chat-header-text">
              <div className="badge">WeeChat</div>
              <h1>Chats</h1>
              <p className="subtitle">Only the threads you belong to are listed.</p>
            </div>
            <div className="chat-user">
              <div className="user-meta">
                <span className="user-login">{auth.profile.login}</span>
                <button className="link" type="button" onClick={handleLogout}>
                  Sign out
                </button>
              </div>
              {auth.profile.avatarUrl ? (
                <img
                  className="avatar"
                  src={auth.profile.avatarUrl}
                  alt={`${auth.profile.login} avatar`}
                />
              ) : (
                <div className="avatar avatar-fallback">
                  {auth.profile.login.slice(0, 2).toUpperCase()}
                </div>
              )}
            </div>
          </header>

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

export default App
