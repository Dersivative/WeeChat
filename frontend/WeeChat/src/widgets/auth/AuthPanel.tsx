import { type FormEvent } from 'react'
import ChildLoginPanel from '../../features/auth/ui/ChildLoginPanel'
import UserLoginPanel from '../../features/auth/ui/UserLoginPanel'
import type { AccountType } from '../../entities/account'

type AuthPanelProps = {
  formState: {
    login: string
    password: string
  }
  role: AccountType
  loginLoading: boolean
  loginError: string | null
  onRoleChange: (role: AccountType) => void
  onFormChange: (field: 'login' | 'password', value: string) => void
  onLoginSubmit: (event: FormEvent<HTMLFormElement>) => void
}

function AuthPanel({
  formState,
  role,
  loginLoading,
  loginError,
  onRoleChange,
  onFormChange,
  onLoginSubmit,
}: AuthPanelProps) {
  return (
    <>
      <section className="role-switch">
        <p className="section-label">Choose sign-in method</p>
        <div className={`switcher ${role === 'child' ? 'child' : ''}`}>
          <span className="switch-pill" />
          <button
            className={role === 'user' ? 'active' : ''}
            type="button"
            onClick={() => onRoleChange('user')}
          >
            Parent
          </button>
          <button
            className={role === 'child' ? 'active' : ''}
            type="button"
            onClick={() => onRoleChange('child')}
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
