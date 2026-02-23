import { type FormEvent, useCallback, useEffect, useState } from 'react'
import type { ChildProfile, ModerationLevel } from '../../entities/child'
import type { AuthFetch } from '../../shared/apiClient'
import LinkChildPanel from './LinkChildPanel'

type ChildrenManagerProps = {
  apiBaseUrl: string
  authFetch: AuthFetch
}

const DEFAULT_LEVEL: ModerationLevel = 'MANUAL'

function ChildrenManager({ apiBaseUrl, authFetch }: ChildrenManagerProps) {
  const [children, setChildren] = useState<ChildProfile[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [displayName, setDisplayName] = useState('')
  const [avatarFileName, setAvatarFileName] = useState('')
  const [moderationLevel, setModerationLevel] = useState<ModerationLevel>(DEFAULT_LEVEL)
  const [saving, setSaving] = useState(false)
  const [deleteCandidateId, setDeleteCandidateId] = useState<string | null>(null)
  const [loginCodeChild, setLoginCodeChild] = useState<ChildProfile | null>(null)
  const [loginCode, setLoginCode] = useState<string | null>(null)
  const [loginCodeExpiresAt, setLoginCodeExpiresAt] = useState<string | null>(null)
  const [loginCodeLoading, setLoginCodeLoading] = useState(false)
  const [loginCodeError, setLoginCodeError] = useState<string | null>(null)
  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(null)

  const loadChildren = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await authFetch(`${apiBaseUrl}/api/children`)
      if (!response.ok) {
        throw new Error('Could not load children.')
      }
      const payload = (await response.json()) as ChildProfile[]
      setChildren(payload)
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Could not load children.')
    } finally {
      setLoading(false)
    }
  }, [apiBaseUrl, authFetch])

  useEffect(() => {
    void loadChildren()
  }, [loadChildren])

  useEffect(() => {
    if (!loginCodeExpiresAt) {
      setRemainingSeconds(null)
      return
    }
    const expiresAtMs = new Date(loginCodeExpiresAt).getTime()
    const updateRemaining = () => {
      const now = Date.now()
      const secondsLeft = Math.max(0, Math.ceil((expiresAtMs - now) / 1000))
      setRemainingSeconds(secondsLeft)
    }
    updateRemaining()
    const interval = window.setInterval(updateRemaining, 1000)
    return () => window.clearInterval(interval)
  }, [loginCodeExpiresAt])

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!displayName.trim()) {
      return
    }
    setSaving(true)
    setError(null)
    try {
      const response = await authFetch(`${apiBaseUrl}/api/children`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          displayName: displayName.trim(),
          avatarFileName: avatarFileName.trim() || null,
          moderationLevel,
        }),
      })
      if (!response.ok) {
        throw new Error('Could not create child.')
      }
      const payload = (await response.json()) as ChildProfile
      setChildren((prev) => [...prev, payload])
      setDisplayName('')
      setAvatarFileName('')
      setModerationLevel(DEFAULT_LEVEL)
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : 'Could not create child.')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (childId: string) => {
    setSaving(true)
    setError(null)
    try {
      const response = await authFetch(`${apiBaseUrl}/api/children/${childId}`, {
        method: 'DELETE',
      })
      if (!response.ok) {
        throw new Error('Could not delete child.')
      }
      setChildren((prev) => prev.filter((child) => child.id !== childId))
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : 'Could not delete child.')
    } finally {
      setSaving(false)
      setDeleteCandidateId(null)
    }
  }

  const handleOpenLoginCode = (child: ChildProfile) => {
    setLoginCodeChild(child)
    setLoginCode(null)
    setLoginCodeExpiresAt(null)
    setLoginCodeError(null)
    setLoginCodeLoading(false)
    setRemainingSeconds(null)
  }

  const handleGenerateLoginCode = async () => {
    if (!loginCodeChild) {
      return
    }
    setLoginCode(null)
    setLoginCodeExpiresAt(null)
    setLoginCodeError(null)
    setLoginCodeLoading(true)
    setRemainingSeconds(null)
    try {
      const response = await authFetch(`${apiBaseUrl}/api/children/${loginCodeChild.id}/login-code`, {
        method: 'POST',
      })
      if (!response.ok) {
        throw new Error('Could not generate login code.')
      }
      const payload = (await response.json()) as { token: string; expiresAt: string }
      setLoginCode(payload.token)
      setLoginCodeExpiresAt(payload.expiresAt)
    } catch (loginError) {
      setLoginCodeError(loginError instanceof Error ? loginError.message : 'Could not generate login code.')
    } finally {
      setLoginCodeLoading(false)
    }
  }

  const handleCloseLoginCode = () => {
    setLoginCodeChild(null)
    setLoginCode(null)
    setLoginCodeExpiresAt(null)
    setLoginCodeError(null)
    setLoginCodeLoading(false)
    setRemainingSeconds(null)
  }

  const handleCopyLoginCode = async () => {
    if (!loginCode) {
      return
    }
    try {
      await navigator.clipboard.writeText(loginCode)
    } catch {
    }
  }

  const handleLinkChild = async (code: string) => {
    const response = await authFetch(`${apiBaseUrl}/api/children/link`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ token: code }),
    })

    if (!response.ok) {
      let errorMessage = 'Could not link child account.'
      try {
        const errorData = await response.json()
        if (errorData && errorData.message) {
          errorMessage = errorData.message
        }
      } catch (e) {
      }
      throw { response: { data: { message: errorMessage } } }
    }

    await loadChildren()
  }

  const isExpired = Boolean(loginCode && remainingSeconds === 0)

  return (
    <>
      <section className="panel-card">
        <header className="panel-header">
          <div>
            <p className="section-label">Children</p>
            <h2>Manage Children</h2>
          </div>
          <button className="ghost" type="button" onClick={loadChildren} disabled={loading}>
            Refresh
          </button>
        </header>

        {error ? <p className="form-error">{error}</p> : null}
        {loading ? <p className="status">Loading children...</p> : null}

        <form className="panel-form" onSubmit={handleCreate}>
          <div className="panel-form-row">
            <label className="field">
              Display name
              <input
                type="text"
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                placeholder="Child name"
                required
              />
            </label>
            <label className="field">
              Avatar file name
              <input
                type="text"
                value={avatarFileName}
                onChange={(event) => setAvatarFileName(event.target.value)}
                placeholder="avatar.png"
              />
            </label>
          </div>
          <label className="field">
            Moderation level
            <select value={moderationLevel} onChange={(event) => setModerationLevel(event.target.value as ModerationLevel)}>
              <option value="NONE">None</option>
              <option value="AUTOMATED">Automated</option>
              <option value="MANUAL">Manual</option>
            </select>
          </label>
          <button className="primary" type="submit" disabled={saving}>
            {saving ? 'Saving...' : 'Add child'}
          </button>
        </form>

        <div className="panel-list">
          {children.length === 0 && !loading ? (
            <p className="status subtle">No children yet.</p>
          ) : null}
          {children.map((child) => (
            <article key={child.id} className="child-card">
              <div className="child-info">
                {child.avatarUrl ? (
                  <img className="avatar" src={child.avatarUrl} alt={`${child.displayName} avatar`} />
                ) : (
                  <div className="avatar avatar-fallback">{child.displayName.slice(0, 2).toUpperCase()}</div>
                )}
                <div>
                  <h3>{child.displayName}</h3>
                  <p className="panel-meta">Moderation: {child.moderationLevel}</p>
                </div>
              </div>
              {deleteCandidateId === child.id ? (
                <div className="child-actions">
                  <button className="ghost" type="button" onClick={() => setDeleteCandidateId(null)} disabled={saving}>
                    Cancel
                  </button>
                  <button className="ghost danger" type="button" onClick={() => handleDelete(child.id)} disabled={saving}>
                    Confirm remove
                  </button>
                </div>
              ) : (
                <div className="child-actions">
                  <button
                    className="ghost success"
                    type="button"
                    onClick={() => handleOpenLoginCode(child)}
                    disabled={saving}
                  >
                    <span className="icon-circle" aria-hidden="true">
                      <svg viewBox="0 0 24 24" aria-hidden="true">
                        <path
                          d="M4.5 12h9.2m0 0-3.4-3.4M13.7 12l-3.4 3.4M13.7 6.5h4.8a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1h-4.8"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                      </svg>
                    </span>
                    Generate login code
                  </button>
                  <button
                    className="ghost danger"
                    type="button"
                    onClick={() => setDeleteCandidateId(child.id)}
                    disabled={saving}
                  >
                    Remove
                  </button>
                </div>
              )}
            </article>
          ))}
        </div>
        {loginCodeChild ? (
          <div className="modal-overlay open" onClick={handleCloseLoginCode}>
            <div
              className="modal-card"
              role="dialog"
              aria-modal="true"
              aria-labelledby="login-code-title"
              onClick={(event) => event.stopPropagation()}
            >
              <p className="section-label">Login code</p>
              <h3 id="login-code-title">Generate code for {loginCodeChild.displayName}</h3>
              <p className="panel-meta">This login code will be valid for 30 seconds. Prepare your child device.</p>
              {loginCodeError ? <p className="form-error">{loginCodeError}</p> : null}
              {loginCodeLoading ? (
                <p className="status">Generating code...</p>
              ) : loginCode ? (
                <div className="code-chip">
                  <span>{loginCode}</span>
                  <button className="ghost" type="button" onClick={handleCopyLoginCode}>
                    Copy
                  </button>
                </div>
              ) : null}
              {remainingSeconds !== null ? <p className="panel-meta">Expires in {remainingSeconds}s.</p> : null}
              <div className="modal-actions">
                <button className="ghost" type="button" onClick={handleCloseLoginCode}>
                  {loginCode ? 'Close' : 'Cancel'}
                </button>
                {loginCode ? (
                  <button
                    className="primary"
                    type="button"
                    onClick={handleGenerateLoginCode}
                    disabled={loginCodeLoading || !isExpired}
                  >
                    Generate again
                  </button>
                ) : (
                  <button className="primary" type="button" onClick={handleGenerateLoginCode} disabled={loginCodeLoading}>
                    I'm ready
                  </button>
                )}
              </div>
            </div>
          </div>
        ) : null}
      </section>

      <LinkChildPanel onLinkSubmit={handleLinkChild} />
    </>
  )
}

export default ChildrenManager