import type { AccountType } from '../../entities/account'
import './menu.css'

type AccountMenuProps = {
  open: boolean
  accountType: AccountType
  login: string
  onClose: () => void
  onLogout: () => void
}

function AccountMenu({ open, accountType, login, onClose, onLogout }: AccountMenuProps) {
  return (
    <>
      <div className={`menu-overlay ${open ? 'open' : ''}`} onClick={onClose} />
      <aside className={`menu-panel ${open ? 'open' : ''}`} role="menu">
        <div className="menu-header">
          <div>
            <p className="menu-title">Account</p>
            <p className="menu-subtitle">{login}</p>
          </div>
          <button className="menu-close" type="button" onClick={onClose} aria-label="Close menu">
            ×
          </button>
        </div>
        <div className="menu-items">
          {accountType === 'user' ? (
            <>
              <button className="menu-item" type="button" disabled>
                Settings
              </button>
              <button className="menu-item" type="button" disabled>
                Moderation Queue
              </button>
            </>
          ) : (
            <button className="menu-item" type="button" disabled>
              Setting
            </button>
          )}
          <button className="menu-item danger" type="button" onClick={onLogout}>
            Log out
          </button>
        </div>
      </aside>
    </>
  )
}

export default AccountMenu
