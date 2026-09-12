import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useWebSocket } from '../context/WebSocketContext'
import { ROLE_LABELS } from '../utils/labels'
import { NotificationCenter } from './NotificationCenter'

const navLinkClass = ({ isActive }) =>
  `rounded-md px-3 py-2 text-sm font-medium transition-colors ${
    isActive ? 'bg-teal-600 text-white' : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
  }`

export function Layout() {
  const { user, logout } = useAuth()
  const { connected } = useWebSocket()
  const canViewAudit = user?.role === 'ADMIN' || user?.role === 'COORDINATOR'

  return (
    <div className="min-h-screen bg-gray-50">
      <NotificationCenter />
      <header className="border-b border-gray-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
          <div className="flex items-center gap-6">
            <span className="text-lg font-semibold text-teal-700">Hjemmesykepleie</span>
            <nav className="flex gap-1">
              <NavLink to="/" end className={navLinkClass}>
                Dashbord
              </NavLink>
              <NavLink to="/kart" className={navLinkClass}>
                Kart
              </NavLink>
              <NavLink to="/fravaer" className={navLinkClass}>
                Fravær
              </NavLink>
              <NavLink to="/journal" className={navLinkClass}>
                Journal
              </NavLink>
              <NavLink to="/rapporter" className={navLinkClass}>
                Rapporter
              </NavLink>
              {canViewAudit && (
                <NavLink to="/revisjonslogg" className={navLinkClass}>
                  Revisjonslogg
                </NavLink>
              )}
            </nav>
          </div>
          <div className="flex items-center gap-4">
            <span className="flex items-center gap-1.5 text-xs text-gray-500">
              <span
                className={`h-2 w-2 rounded-full ${connected ? 'bg-emerald-500' : 'bg-gray-300'}`}
                title={connected ? 'Tilkoblet sanntidsoppdateringer' : 'Ikke tilkoblet'}
              />
              {connected ? 'Sanntid tilkoblet' : 'Kobler til …'}
            </span>
            <div className="text-right text-sm">
              <div className="font-medium text-gray-800">{user?.fullName}</div>
              <div className="text-xs text-gray-500">{ROLE_LABELS[user?.role] ?? user?.role}</div>
            </div>
            <button
              type="button"
              onClick={logout}
              className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100"
            >
              Logg ut
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
