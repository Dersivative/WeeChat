import { type FormEvent, useState } from 'react'

type ChildLoginPanelProps = {
  onBackupSubmit?: (code: string) => void
  loading?: boolean
  error?: string | null
}

function ChildLoginPanel({ onBackupSubmit, loading = false, error = null }: ChildLoginPanelProps) {
  const [backupCode, setBackupCode] = useState('')

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!onBackupSubmit) {
      return
    }
    onBackupSubmit(backupCode.trim())
  }

  return (
    <section className="form-panel">
      <h2>Child login</h2>
      <p className="hint">Paste the login code generated in the parent panel.</p>
      <div className="child-login">
        <div className="qr-card">
          <div className="qr-frame">
            <span className="qr-dot" />
          </div>
          <p>QR login coming soon</p>
          <button className="ghost" type="button" disabled>
            Start scanning
          </button>
        </div>
        <form className="code-card form-grid" onSubmit={handleSubmit}>
          <label className="field">
            Login code
            <input
              type="text"
              placeholder="Paste login code"
              value={backupCode}
              onChange={(event) => setBackupCode(event.target.value)}
              required
            />
          </label>
          {error ? <p className="form-error">{error}</p> : null}
          <button className="primary" type="submit" disabled={loading || !onBackupSubmit}>
            {loading ? 'Checking...' : 'Continue'}
          </button>
        </form>
      </div>
    </section>
  )
}

export default ChildLoginPanel
