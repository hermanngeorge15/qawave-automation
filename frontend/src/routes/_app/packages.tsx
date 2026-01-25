import { useState } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { usePackages } from '@/hooks'
import { PackageCard } from '@/components/packages'
import { PackagesListSkeleton, EmptyState } from '@/components/ui'

export const Route = createFileRoute('/_app/packages')({
  component: PackagesPage,
})

function PackagesPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const pageSize = 12

  const { data, isLoading, isError, error } = usePackages(page, pageSize)

  // Filter packages by search term (client-side for now)
  const filteredPackages =
    data?.content.filter(
      (pkg) =>
        pkg.name.toLowerCase().includes(search.toLowerCase()) ||
        pkg.description?.toLowerCase().includes(search.toLowerCase())
    ) ?? []

  return (
    <div className="packages-page">
      <header className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
        <div>
          <h1 className="page-title">QA Packages</h1>
          <p className="text-secondary-400">Manage your QA automation packages</p>
        </div>
        <Link to="/packages/new" className="btn btn-primary">
          + New Package
        </Link>
      </header>

      {/* Search */}
      <div className="mb-6">
        <input
          type="text"
          placeholder="Search packages..."
          value={search}
          onChange={(e) => {
            setSearch(e.target.value)
          }}
          className="w-full sm:w-80 px-4 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white placeholder-secondary-500 focus:outline-none focus:border-primary-500 transition-colors"
        />
      </div>

      {/* Content */}
      {isLoading ? (
        <PackagesListSkeleton />
      ) : isError ? (
        <div className="error-state">
          <h2>Error loading packages</h2>
          <p>{error.message}</p>
          <button
            onClick={() => {
              window.location.reload()
            }}
            className="btn btn-primary mt-4"
          >
            Retry
          </button>
        </div>
      ) : filteredPackages.length === 0 ? (
        <EmptyState
          title={search ? 'No packages found' : 'No packages yet'}
          description={
            search
              ? 'Try adjusting your search terms'
              : 'Create your first QA package to get started with automated testing'
          }
          action={
            !search && (
              <Link to="/packages/new" className="btn btn-primary">
                Create Package
              </Link>
            )
          }
          icon={
            <svg
              className="w-full h-full"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
              />
            </svg>
          }
        />
      ) : (
        <>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {filteredPackages.map((pkg) => (
              <PackageCard key={pkg.id} pkg={pkg} />
            ))}
          </div>

          {/* Pagination */}
          {data && data.totalPages > 1 && (
            <div className="flex justify-center items-center gap-4 mt-8">
              <button
                onClick={() => {
                  setPage((p) => Math.max(0, p - 1))
                }}
                disabled={data.first}
                className="btn btn-ghost disabled:opacity-50"
              >
                Previous
              </button>
              <span className="text-secondary-400">
                Page {page + 1} of {data.totalPages}
              </span>
              <button
                onClick={() => {
                  setPage((p) => p + 1)
                }}
                disabled={data.last}
                className="btn btn-ghost disabled:opacity-50"
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
