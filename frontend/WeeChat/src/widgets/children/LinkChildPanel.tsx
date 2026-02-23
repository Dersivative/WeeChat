import { type FormEvent, useState } from 'react'

type LinkChildPanelProps = {
  onLinkSubmit: (code: string) => Promise<void>
  loading?: boolean
}

function LinkChildPanel({ onLinkSubmit, loading = false }: LinkChildPanelProps) {
  const [code, setCode] = useState('')
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    
    try {
      await onLinkSubmit(code.trim())
      setCode('') 
    } catch (err: any) {
      setError(err.response?.data?.message || 'Nie udało się przypisać konta. Sprawdź kod.')
    }
  }

  return (
    <section className="form-panel mt-4">
      <h2>Add existing child account</h2>
      <p className="hint">Ask the other parent to generate a login code and paste it below to link the account.</p>
      
      <div className="child-login">
        <form className="code-card form-grid" onSubmit={handleSubmit}>
          <label className="field">
            Linking code
            <input
              type="text"
              placeholder="Paste 6-digit code"
              value={code}
              onChange={(event) => setCode(event.target.value)}
              required
            />
          </label>
          {error ? <p className="form-error">{error}</p> : null}
          <button className="primary" type="submit" disabled={loading || !code.trim()}>
            {loading ? 'Linking...' : 'Link account'}
          </button>
        </form>
      </div>
    </section>
  )
}

export default LinkChildPanel