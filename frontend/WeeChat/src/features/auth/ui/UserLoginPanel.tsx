import type { FormEvent } from 'react'

type UserLoginPanelProps = {
  formState: {
    login: string
    password: string
  }
  onFormChange: (field: 'login' | 'password', value: string) => void
  onLoginSubmit: (event: FormEvent<HTMLFormElement>) => void
  loginLoading: boolean
  loginError: string | null
}

function UserLoginPanel({
  formState,
  onFormChange,
  onLoginSubmit,
  loginLoading,
  loginError,
}: UserLoginPanelProps) {
  return (
    <section className="form-panel">
      <h2>Login</h2>
      <p className="hint">Use the account credentials from the backend seed data.</p>
      <form className="form-grid" onSubmit={onLoginSubmit}>
        <label className="field">
          Username
          <input
            type="text"
            placeholder="alice"
            value={formState.login}
            onChange={(event) => onFormChange('login', event.target.value)}
            required
          />
        </label>
        <label className="field">
          Password
          <input
            type="password"
            placeholder="hash-alice"
            value={formState.password}
            onChange={(event) => onFormChange('password', event.target.value)}
            required
          />
        </label>
        {loginError ? <p className="form-error">{loginError}</p> : null}
        <button className="primary" type="submit" disabled={loginLoading}>
          {loginLoading ? 'Signing in...' : 'Continue'}
        </button>
      </form>
    </section>
  )
}

export default UserLoginPanel
