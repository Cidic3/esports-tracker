import { Link, NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { TeamSearch } from './TeamSearch'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-2 text-sm font-medium transition-colors ${
    isActive ? 'bg-zinc-800 text-white' : 'text-zinc-400 hover:bg-zinc-800/60 hover:text-zinc-100'
  }`

export function Layout() {
  const { isAuthenticated, user, logout } = useAuth()

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100">
      <header className="sticky top-0 z-10 border-b border-zinc-800 bg-zinc-950/90 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center gap-6 px-4 py-3">
          <Link to="/" className="flex items-center gap-2 text-lg font-bold tracking-tight">
            <span className="text-violet-400">▲</span> Esports Tracker
          </Link>
          <nav className="flex flex-1 items-center gap-1">
            {isAuthenticated && (
              <NavLink to="/" end className={navLinkClass}>
                Feed
              </NavLink>
            )}
            <NavLink to="/matches" className={navLinkClass}>
              Matches
            </NavLink>
            <NavLink to="/tournaments" className={navLinkClass}>
              Tournaments
            </NavLink>
            {isAuthenticated && (
              <NavLink to="/settings" className={navLinkClass}>
                Following
              </NavLink>
            )}
          </nav>
          <TeamSearch />
          <div className="flex items-center gap-3">
            {isAuthenticated ? (
              <>
                <span className="text-sm text-zinc-400">{user?.username}</span>
                <button
                  onClick={logout}
                  className="rounded-md border border-zinc-700 px-3 py-1.5 text-sm text-zinc-300 transition-colors hover:border-zinc-500 hover:text-white"
                >
                  Log out
                </button>
              </>
            ) : (
              <>
                <Link
                  to="/login"
                  className="rounded-md px-3 py-1.5 text-sm text-zinc-300 hover:text-white"
                >
                  Log in
                </Link>
                <Link
                  to="/register"
                  className="rounded-md bg-violet-600 px-3 py-1.5 text-sm font-medium text-white transition-colors hover:bg-violet-500"
                >
                  Sign up
                </Link>
              </>
            )}
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
