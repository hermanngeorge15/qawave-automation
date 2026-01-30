import { useState, type ReactNode } from 'react'
import { Link } from '@tanstack/react-router'
import { useAuth } from '@/lib/auth'

interface MainLayoutProps {
  children: ReactNode
}

export function MainLayout({ children }: MainLayoutProps) {
  const { user, logout, isAuthenticated } = useAuth()
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false)

  return (
    <div className="main-layout">
      <header className="main-header">
        <nav className="main-nav">
          <Link to="/" className="nav-logo">
            QAWave
          </Link>
          <div className="flex items-center gap-6">
            <div className="nav-links">
              <Link
                to="/packages"
                className="nav-link"
                activeProps={{ className: 'nav-link active' }}
              >
                Packages
              </Link>
              <Link
                to="/scenarios"
                className="nav-link"
                activeProps={{ className: 'nav-link active' }}
              >
                Scenarios
              </Link>
              <Link
                to="/settings"
                className="nav-link"
                activeProps={{ className: 'nav-link active' }}
              >
                Settings
              </Link>
            </div>

            {/* User Menu */}
            {isAuthenticated && user && (
              <div className="relative">
                <button
                  onClick={() => { setIsUserMenuOpen((prev) => !prev) }}
                  className="flex items-center gap-2 px-3 py-1.5 rounded-lg hover:bg-secondary-800 transition-colors"
                >
                  <div className="w-8 h-8 rounded-full bg-primary-600 flex items-center justify-center text-white font-medium">
                    {user.name.charAt(0).toUpperCase() || user.email.charAt(0).toUpperCase()}
                  </div>
                  <span className="text-sm text-secondary-300 hidden sm:block">
                    {user.name || user.email}
                  </span>
                  <svg
                    className={`w-4 h-4 text-secondary-400 transition-transform ${isUserMenuOpen ? 'rotate-180' : ''}`}
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                  </svg>
                </button>

                {isUserMenuOpen && (
                  <>
                    {/* Backdrop */}
                    <div
                      className="fixed inset-0 z-10"
                      onClick={() => { setIsUserMenuOpen(false) }}
                    />

                    {/* Dropdown */}
                    <div className="absolute right-0 mt-2 w-56 bg-secondary-800 border border-secondary-700 rounded-lg shadow-lg z-20">
                      <div className="px-4 py-3 border-b border-secondary-700">
                        <p className="text-sm text-white font-medium">{user.name}</p>
                        <p className="text-xs text-secondary-400 truncate">{user.email}</p>
                      </div>
                      <div className="py-1">
                        <Link
                          to="/settings"
                          className="block px-4 py-2 text-sm text-secondary-300 hover:bg-secondary-700 hover:text-white transition-colors"
                          onClick={() => { setIsUserMenuOpen(false) }}
                        >
                          Settings
                        </Link>
                        <button
                          onClick={() => {
                            setIsUserMenuOpen(false)
                            logout()
                          }}
                          className="w-full text-left px-4 py-2 text-sm text-red-400 hover:bg-secondary-700 hover:text-red-300 transition-colors"
                        >
                          Sign Out
                        </button>
                      </div>
                    </div>
                  </>
                )}
              </div>
            )}
          </div>
        </nav>
      </header>
      <main className="main-content">{children}</main>
    </div>
  )
}
