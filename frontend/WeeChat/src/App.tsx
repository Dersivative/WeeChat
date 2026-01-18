import { useState } from 'react'
import './App.css'

function App() {
  const [role, setRole] = useState<'parent' | 'child'>('parent')

  return (
    <div className="app">
      <main className="welcome-card">
        <header className="welcome-header">
          <div className="badge">WeeChat</div>
          <h1>Welcome back!</h1>
          <p className="subtitle">
            A safe, playful space for families to chat together.
          </p>
        </header>

        <section className="role-switch">
          <p className="section-label">I am logging in as:</p>
          <div className={`switcher ${role}`}>
            <button
              className={role === 'parent' ? 'active' : ''}
              onClick={() => setRole('parent')}
              type="button"
              aria-pressed={role === 'parent'}
            >
              Parent
            </button>
            <button
              className={role === 'child' ? 'active' : ''}
              onClick={() => setRole('child')}
              type="button"
              aria-pressed={role === 'child'}
            >
              Child
            </button>
            <span className="switch-pill" aria-hidden="true" />
          </div>
        </section>

        {role === 'parent' ? (
          <section className="form-panel">
            <h2>Parent login</h2>
            <p className="hint">
              Use your account details. Two-factor will be offered next.
            </p>
            <form className="form-grid">
              <label className="field">
                Username
                <input type="text" placeholder="parent@example.com" />
              </label>
              <label className="field">
                Password
                <input type="password" placeholder="Your secure password" />
              </label>
              <div className="row">
                <label className="checkbox">
                  <input type="checkbox" />
                  Remember me
                </label>
                <button className="link" type="button">
                  Forgot password?
                </button>
              </div>
              <button className="primary" type="button">
                Continue
              </button>
            </form>
          </section>
        ) : (
          <section className="form-panel">
            <h2>Child login</h2>
            <p className="hint">
              Scan the QR code your parent created, or type the backup code.
            </p>
            <div className="child-login">
              <div className="qr-card">
                <div className="qr-frame">
                  <div className="qr-dot" />
                  <div className="qr-dot" />
                  <div className="qr-dot" />
                  <div className="qr-dot" />
                </div>
                <p>Point the camera here</p>
              </div>
              <div className="code-card">
                <label className="field">
                  Backup code
                  <input type="text" placeholder="ABC-123-XYZ" />
                </label>
                <button className="primary" type="button">
                  Start chatting
                </button>
                <button className="ghost" type="button">
                  Need help from a parent?
                </button>
              </div>
            </div>
          </section>
        )}
      </main>
    </div>
  )
}

export default App
