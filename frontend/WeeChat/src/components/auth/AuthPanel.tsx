import { type FormEvent, useState } from 'react'
import ChildLoginPanel from './ChildLoginPanel'
import UserLoginPanel from './UserLoginPanel'

type AuthPanelProps = {
  formState: {
    login: string
    password: string
  }
  loginLoading: boolean
  loginError: string | null
  onFormChange: (field: 'login' | 'password', value: string) => void
  onLoginSubmit: (event: FormEvent<HTMLFormElement>) => void
}

type LoginRole = 'user' | 'child'

function AuthPanel({
  formState,
  loginLoading,
  loginError,
  onFormChange,
  onLoginSubmit,
}: AuthPanelProps) {
  const [role, setRole] = useState<LoginRole>('user')

  return (
    <>
      <section className="role-switch">
        <p className="section-label">Choose sign-in method</p>
        <div className={`switcher ${role === 'child' ? 'child' : ''}`}>
          <span className="switch-pill" />
          <button
            className={role === 'user' ? 'active' : ''}
            type="button"
            onClick={() => setRole('user')}
          >
            Parent
          </button>
          <button
            className={role === 'child' ? 'active' : ''}
            type="button"
            onClick={() => setRole('child')}
          >
            Child
          </button>
        </div>
      </section>

      {role === 'user' ? (
        <UserLoginPanel
          formState={formState}
          loginError={loginError}
          loginLoading={loginLoading}
          onFormChange={onFormChange}
          onLoginSubmit={onLoginSubmit}
        />
      ) : (
        <ChildLoginPanel />
      )}
    </>
  )
}

export default AuthPanel
