import { useCallback, useEffect, useState } from 'react'
import type { ChildProfile, ModerationLevel } from '../../entities/child'
import type { ModerationLevelOption } from '../../entities/moderation'
import type { AuthFetch } from '../../shared/apiClient'

type ModerationSettingsPanelProps = {
  apiBaseUrl: string
  authFetch: AuthFetch
}

const LEVEL_OPTIONS: ModerationLevelOption[] = [
  {
    value: 'NONE',
    label: 'None',
    description: 'All incoming messages are approved automatically.',
  },
  {
    value: 'AUTOMATED',
    label: 'Automated',
    description: 'Model signals decide whether to approve or reject.',
  },
  {
    value: 'MANUAL',
    label: 'Manual',
    description: 'Parent reviews every incoming message with LLM hints.',
  },
]

function ModerationSettingsPanel({ apiBaseUrl, authFetch }: ModerationSettingsPanelProps) {
  const [children, setChildren] = useState<ChildProfile[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [selectedLevel, setSelectedLevel] = useState<ModerationLevel>('MANUAL')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

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
      if (!selectedId && payload.length > 0) {
        setSelectedId(payload[0].id)
        setSelectedLevel(payload[0].moderationLevel)
      }
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Could not load children.')
    } finally {
      setLoading(false)
    }
  }, [apiBaseUrl, authFetch, selectedId])

  useEffect(() => {
    void loadChildren()
  }, [loadChildren])

  useEffect(() => {
    const selected = children.find((child) => child.id === selectedId)
    if (selected) {
      setSelectedLevel(selected.moderationLevel)
    }
  }, [selectedId, children])

  const handleSave = async () => {
    if (!selectedId) {
      return
    }
    setSaving(true)
    setError(null)
    try {
      const response = await authFetch(`${apiBaseUrl}/api/children/${selectedId}/moderation-level`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ moderationLevel: selectedLevel }),
      })
      if (!response.ok) {
        throw new Error('Could not update moderation level.')
      }
      const payload = (await response.json()) as ChildProfile
      setChildren((prev) => prev.map((child) => (child.id === payload.id ? payload : child)))
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : 'Could not update moderation level.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="panel-card">
      <header className="panel-header">
        <div>
          <p className="section-label">Moderation</p>
          <h2>Moderation Settings</h2>
        </div>
        <button className="ghost" type="button" onClick={loadChildren} disabled={loading}>
          Refresh
        </button>
      </header>

      {error ? <p className="form-error">{error}</p> : null}
      {loading ? <p className="status">Loading children...</p> : null}

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
        </div>
        <div className="panel-content">
          {selectedId ? (
            <>
              <p className="panel-meta">Moderation level</p>
              <div className="panel-options">
                {LEVEL_OPTIONS.map((option) => (
                  <label key={option.value} className="option-card">
                    <input
                      type="radio"
                      name="moderationLevel"
                      value={option.value}
                      checked={selectedLevel === option.value}
                      onChange={() => setSelectedLevel(option.value)}
                    />
                    <div>
                      <h3>{option.label}</h3>
                      <p>{option.description}</p>
                    </div>
                  </label>
                ))}
              </div>
              <button className="primary" type="button" onClick={handleSave} disabled={saving}>
                {saving ? 'Saving...' : 'Save settings'}
              </button>
            </>
          ) : (
            <p className="status subtle">Select a child to edit moderation.</p>
          )}
        </div>
      </div>
    </section>
  )
}

export default ModerationSettingsPanel
