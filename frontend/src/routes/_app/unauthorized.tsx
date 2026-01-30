import { createFileRoute, Link } from '@tanstack/react-router'
import { useAuth } from '@/lib/auth'

export const Route = createFileRoute('/_app/unauthorized')({
  component: UnauthorizedPage,
})

function UnauthorizedPage() {
  const { user, logout } = useAuth()

  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center">
      <div className="text-center">
        <div className="text-6xl mb-4">🚫</div>
        <h1 className="text-3xl font-bold text-white mb-4">Access Denied</h1>
        <p className="text-secondary-400 mb-6 max-w-md">
          You don't have permission to access this page.
          {user && (
            <span className="block mt-2">
              Logged in as: <strong className="text-white">{user.email}</strong>
            </span>
          )}
        </p>
        <div className="flex gap-4 justify-center">
          <Link to="/packages" className="btn btn-primary">
            Go to Packages
          </Link>
          <button onClick={logout} className="btn btn-ghost">
            Sign Out
          </button>
        </div>
      </div>
    </div>
  )
}
