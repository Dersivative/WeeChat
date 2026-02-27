import { type FormEvent, useEffect, useState } from 'react'
import ChildLoginPanel from '../../features/auth/ui/ChildLoginPanel'
import UserLoginPanel from '../../features/auth/ui/UserLoginPanel'
import type { AccountType } from '../../entities/account'
import type { RegisterRequest } from '../../shared/apiClient'

type AuthPanelProps = {
  formState: {
    login: string
    password: string
    email?: string
    displayName?: string
  }
  role: AccountType
  loginLoading: boolean
  loginError: string | null
  childLoginLoading: boolean
  childLoginError: string | null
  registerPath?: boolean
  onNavigateToRegister?: () => void
  onNavigateToLogin?: () => void
  onRoleChange: (role: AccountType) => void
  onFormChange: (field: string, value: string) => void
  onLoginSubmit: (event: FormEvent<HTMLFormElement>) => void
  onRegisterSubmit: (data: RegisterRequest) => void
  onChildLoginSubmit: (code: string) => void
}

function AuthPanel({
  formState,
  role,
  loginLoading,
  loginError,
  childLoginLoading,
  childLoginError,
  registerPath = false,
  onNavigateToRegister,
  onNavigateToLogin,
  onRoleChange,
  onFormChange,
  onLoginSubmit,
  onRegisterSubmit,
  onChildLoginSubmit,
}: AuthPanelProps) {
  const [isRegistering, setIsRegistering] = useState(registerPath)
  useEffect(() => {
    setIsRegistering(registerPath)
  }, [registerPath])

  const handleUserSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    
    if (isRegisteringMode) {
      onRegisterSubmit({
        login: formState.login,
        password: formState.password,
        email: formState.email || '',
        displayName: formState.displayName || ''
      })
    } else {
      onLoginSubmit(event)
    }
  }

  const handleRoleChange = (newRole: AccountType) => {
    setIsRegistering(false)
    onRoleChange(newRole)
  }

  const isRegisteringMode = registerPath || isRegistering

  return (
    <>
      <section className="role-switch">
        <p className="section-label">Choose sign-in method</p>
        <div className={`switcher ${role === 'child' ? 'child' : ''}`}>
          <span className="switch-pill" />
          <button
            className={role === 'user' ? 'active' : ''}
            type="button"
            onClick={() => handleRoleChange('user')}
          >
            Parent
          </button>
          <button
            className={role === 'child' ? 'active' : ''}
            type="button"
            onClick={() => handleRoleChange('child')}
          >
            Child
          </button>
        </div>
      </section>

      {role === 'user' ? (
        <UserLoginPanel
          formState={formState}
          error={loginError}
          loading={loginLoading}
          onFormChange={onFormChange}
          onSubmit={handleUserSubmit}
          isRegistering={isRegisteringMode}
          onToggleMode={() => setIsRegistering(!isRegistering)}
          onNavigateToRegister={onNavigateToRegister}
          onNavigateToLogin={onNavigateToLogin}
        />
      ) : (
        <ChildLoginPanel 
          onBackupSubmit={onChildLoginSubmit} 
          loading={childLoginLoading} 
          error={childLoginError} 
        />
      )}
    </>
  )
}

export default AuthPanel