import type { ReactNode } from 'react'
import { Link } from '@tanstack/react-router'

interface MainLayoutProps {
  children: ReactNode
}

export function MainLayout({ children }: MainLayoutProps) {
  return (
    <div className="main-layout">
      <header className="main-header">
        <nav className="main-nav">
          <Link to="/" className="nav-logo">
            QAWave
          </Link>
          <div className="nav-links">
            <Link
              to="/packages"
              className="nav-link"
              activeProps={{ className: 'nav-link active' }}
            >
              Packages
            </Link>
            <Link
              to="/settings"
              className="nav-link"
              activeProps={{ className: 'nav-link active' }}
            >
              Settings
            </Link>
          </div>
        </nav>
      </header>
      <main className="main-content">{children}</main>
    </div>
  )
}
