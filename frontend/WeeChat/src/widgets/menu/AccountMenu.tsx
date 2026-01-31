import type { AccountType } from '../../entities/account'
import './menu.css'

type AccountMenuProps = {
  open: boolean
  accountType: AccountType
  login: string
  onClose: () => void
  onLogout: () => void
  onSelectPanel: (panel: AccountPanel) => void
}

export type AccountPanel = 'chats' | 'manage-children' | 'moderation-settings' | 'moderation-queue'

function AccountMenu({
  open,
  accountType,
  login,
  onClose,
  onLogout,
  onSelectPanel,
}: AccountMenuProps) {
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
              <div className="menu-section">
                <button className="menu-item" type="button" onClick={() => onSelectPanel('chats')}>
                  Chats
                </button>
              </div>
              <hr className="menu-separator" />
              <div className="menu-section">
                <p className="menu-section-title">Children</p>
                <button className="menu-item" type="button" onClick={() => onSelectPanel('manage-children')}>
                  Manage Children
                </button>
                <button className="menu-item" type="button" onClick={() => onSelectPanel('moderation-settings')}>
                  Moderation Settings
                </button>
                <button className="menu-item" type="button" onClick={() => onSelectPanel('moderation-queue')}>
                  Moderatation Panel
                </button>
              </div>
              <hr className="menu-separator" />
              <div className="menu-section">
                <button className="menu-item danger" type="button" onClick={onLogout}>
                  Log out
                </button>
              </div>
            </>
          ) : (
            <>
              <div className="menu-section">
                <button className="menu-item" type="button" onClick={() => onSelectPanel('chats')}>
                  Chats
                </button>
              </div>
              <hr className="menu-separator" />
              <div className="menu-section">
                <button className="menu-item danger" type="button" onClick={onLogout}>
                  Log out
                </button>
              </div>
            </>
          )}
        </div>
      </aside>
    </>
  )
}

export default AccountMenu
