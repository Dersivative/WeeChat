import type { FormEvent } from 'react'

type UserLoginPanelProps = {
  formState: {
    login: string
    password: string
    email?: string        
    displayName?: string  
  }
  isRegistering: boolean  
  onFormChange: (field: string, value: string) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onToggleMode: () => void 
  onNavigateToRegister?: () => void
  onNavigateToLogin?: () => void
  loading: boolean
  error: string | null
}

function UserLoginPanel({
  formState,
  isRegistering,
  onFormChange,
  onSubmit,
  onToggleMode,
  onNavigateToRegister,
  onNavigateToLogin,
  loading,
  error,
}: UserLoginPanelProps) {
  const handleToggleMode = () => {
    if (isRegistering && onNavigateToLogin) onNavigateToLogin()
    else if (!isRegistering && onNavigateToRegister) onNavigateToRegister()
    else onToggleMode()
  }
  return (
    <section className="form-panel">
      <h2>{isRegistering ? 'Create Account' : 'Login'}</h2>
      <p className="hint">
        {isRegistering 
          ? 'Join as a parent to manage your family.' 
          : 'Type in your credentials.'}
      </p>
      
      <form className="form-grid" onSubmit={onSubmit}>
        {isRegistering && (
          <>
            <label className="field">
              Display Name
              <input
                type="text"
                placeholder="e.g. Alice"
                value={formState.displayName || ''}
                onChange={(e) => onFormChange('displayName', e.target.value)}
                required
              />
            </label>
            <label className="field">
              Email
              <input
                type="email"
                placeholder="alice@example.com"
                value={formState.email || ''}
                onChange={(e) => onFormChange('email', e.target.value)}
                required
              />
            </label>
          </>
        )}

        <label className="field">
          Username
          <input
            type="text"
            placeholder="Login"
            value={formState.login}
            onChange={(e) => onFormChange('login', e.target.value)}
            required
          />
        </label>
        <label className="field">
          Password
          <input
            type="password"
            placeholder="Password"
            value={formState.password}
            onChange={(e) => onFormChange('password', e.target.value)}
            required
          />
        </label>

        {error ? <p className="form-error" role="alert">{error}</p> : null}
        
        <button className="primary" type="submit" disabled={loading}>
          {loading ? 'Processing...' : (isRegistering ? 'Sign Up' : 'Continue')}
        </button>

        <button 
          type="button" 
          className="link-button" 
          onClick={handleToggleMode}
          style={{ marginTop: '1rem', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--primary-color)' }}
        >
          {isRegistering 
            ? 'Already have an account? Sign in' 
            : 'No account? Create one'}
        </button>
      </form>
    </section>
  )
}

export default UserLoginPanel