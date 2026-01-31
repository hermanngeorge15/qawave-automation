import { useState, useMemo } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { usePackages } from '@/hooks'
import { PackageCard, CreatePackageModal } from '@/components/packages'
import { PackagesListSkeleton, EmptyState, StatusBadge } from '@/components/ui'
import type { QaPackage, QaPackageStatus } from '@/api/types'

export const Route = createFileRoute('/_app/packages')({
  component: PackagesPage,
})

type SortOption = 'updated' | 'name' | 'created' | 'status'
type ViewMode = 'grid' | 'list'

const STATUS_OPTIONS: { value: QaPackageStatus | 'all'; label: string }[] = [
  { value: 'all', label: 'All Statuses' },
  { value: 'READY', label: 'Ready' },
  { value: 'RUNNING', label: 'Running' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'GENERATING', label: 'Generating' },
]

const SORT_OPTIONS: { value: SortOption; label: string }[] = [
  { value: 'updated', label: 'Recently Updated' },
  { value: 'created', label: 'Recently Created' },
  { value: 'name', label: 'Name (A-Z)' },
  { value: 'status', label: 'Status' },
]

const PAGE_SIZE_OPTIONS = [6, 12, 24, 48]

function PackagesPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<QaPackageStatus | 'all'>('all')
  const [sortBy, setSortBy] = useState<SortOption>('updated')
  const [viewMode, setViewMode] = useState<ViewMode>('grid')
  const [pageSize, setPageSize] = useState(12)
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)

  const { data, isLoading, isError, error } = usePackages(page, pageSize)

  // Filter and sort packages (client-side for now)
  const filteredAndSortedPackages = useMemo(() => {
    if (!data?.content) return []

    let result = [...data.content]

    // Apply search filter
    if (search) {
      const searchLower = search.toLowerCase()
      result = result.filter(
        (pkg) =>
          pkg.name.toLowerCase().includes(searchLower) ||
          pkg.description?.toLowerCase().includes(searchLower)
      )
    }

    // Apply status filter
    if (statusFilter !== 'all') {
      result = result.filter((pkg) => pkg.status === statusFilter)
    }

    // Apply sorting
    result.sort((a, b) => {
      switch (sortBy) {
        case 'updated':
          return new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
        case 'created':
          return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        case 'name':
          return a.name.localeCompare(b.name)
        case 'status':
          return a.status.localeCompare(b.status)
        default:
          return 0
      }
    })

    return result
  }, [data?.content, search, statusFilter, sortBy])

  const handlePageSizeChange = (newSize: number) => {
    setPageSize(newSize)
    setPage(0) // Reset to first page when changing page size
  }

  return (
    <div className="packages-page">
      <header className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
        <div>
          <h1 className="page-title">QA Packages</h1>
          <p className="text-secondary-400">Manage your QA automation packages</p>
        </div>
        <button
          onClick={() => {
            setIsCreateModalOpen(true)
          }}
          className="btn btn-primary"
        >
          + New Package
        </button>
      </header>

      {/* Filters and Controls */}
      <div className="bg-secondary-800/50 rounded-lg p-4 mb-6">
        <div className="flex flex-col lg:flex-row gap-4">
          {/* Search */}
          <div className="flex-1">
            <input
              type="text"
              placeholder="Search packages..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value)
              }}
              className="w-full px-4 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white placeholder-secondary-500 focus:outline-none focus:border-primary-500 transition-colors"
            />
          </div>

          {/* Status Filter */}
          <div className="flex gap-4">
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value as QaPackageStatus | 'all')
              }}
              className="px-4 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white focus:outline-none focus:border-primary-500 transition-colors"
            >
              {STATUS_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>

            {/* Sort */}
            <select
              value={sortBy}
              onChange={(e) => {
                setSortBy(e.target.value as SortOption)
              }}
              className="px-4 py-2 bg-secondary-800 border border-secondary-700 rounded-lg text-white focus:outline-none focus:border-primary-500 transition-colors"
            >
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>

            {/* View Toggle */}
            <div className="flex bg-secondary-800 border border-secondary-700 rounded-lg overflow-hidden">
              <button
                onClick={() => {
                  setViewMode('grid')
                }}
                className={`px-3 py-2 ${viewMode === 'grid' ? 'bg-primary-600 text-white' : 'text-secondary-400 hover:text-white'}`}
                title="Grid view"
              >
                <GridIcon />
              </button>
              <button
                onClick={() => {
                  setViewMode('list')
                }}
                className={`px-3 py-2 ${viewMode === 'list' ? 'bg-primary-600 text-white' : 'text-secondary-400 hover:text-white'}`}
                title="List view"
              >
                <ListIcon />
              </button>
            </div>
          </div>
        </div>

        {/* Results count */}
        {!isLoading && data && (
          <div className="mt-3 text-sm text-secondary-400">
            Showing {filteredAndSortedPackages.length} of {data.totalElements} packages
            {(search || statusFilter !== 'all') && ' (filtered)'}
          </div>
        )}
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
      ) : filteredAndSortedPackages.length === 0 ? (
        <EmptyState
          title={search || statusFilter !== 'all' ? 'No packages found' : 'No packages yet'}
          description={
            search || statusFilter !== 'all'
              ? 'Try adjusting your filters'
              : 'Create your first QA package to get started with automated testing'
          }
          action={
            !search &&
            statusFilter === 'all' && (
              <button
                onClick={() => {
                  setIsCreateModalOpen(true)
                }}
                className="btn btn-primary"
              >
                Create Package
              </button>
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
      ) : viewMode === 'grid' ? (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filteredAndSortedPackages.map((pkg) => (
            <PackageCard key={pkg.id} pkg={pkg} />
          ))}
        </div>
      ) : (
        <div className="space-y-2">
          {filteredAndSortedPackages.map((pkg) => (
            <PackageListItem key={pkg.id} pkg={pkg} />
          ))}
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex flex-col sm:flex-row justify-between items-center gap-4 mt-8 pt-4 border-t border-secondary-700">
          <div className="flex items-center gap-2">
            <span className="text-sm text-secondary-400">Show:</span>
            <select
              value={pageSize}
              onChange={(e) => {
                handlePageSizeChange(Number(e.target.value))
              }}
              className="px-3 py-1 bg-secondary-800 border border-secondary-700 rounded-lg text-white text-sm focus:outline-none focus:border-primary-500"
            >
              {PAGE_SIZE_OPTIONS.map((size) => (
                <option key={size} value={size}>
                  {size}
                </option>
              ))}
            </select>
            <span className="text-sm text-secondary-400">per page</span>
          </div>

          <div className="flex items-center gap-4">
            <button
              onClick={() => {
                setPage((p) => Math.max(0, p - 1))
              }}
              disabled={data.first}
              className="btn btn-ghost disabled:opacity-50"
            >
              ← Previous
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
              Next →
            </button>
          </div>
        </div>
      )}

      {/* Create Package Modal */}
      <CreatePackageModal
        isOpen={isCreateModalOpen}
        onClose={() => {
          setIsCreateModalOpen(false)
        }}
      />
    </div>
  )
}

// List view item component
interface PackageListItemProps {
  pkg: QaPackage
}

function PackageListItem({ pkg }: PackageListItemProps) {
  return (
    <Link
      to="/packages/$packageId"
      params={{ packageId: pkg.id }}
      className="flex items-center gap-4 p-4 bg-secondary-800/50 rounded-lg hover:bg-secondary-800 transition-colors"
    >
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-3">
          <h3 className="font-medium text-white truncate">{pkg.name}</h3>
          <StatusBadge status={pkg.status} />
        </div>
        {pkg.description && (
          <p className="text-sm text-secondary-400 truncate mt-1">{pkg.description}</p>
        )}
      </div>
      <div className="text-sm text-secondary-500 shrink-0">
        {formatDate(pkg.updatedAt)}
      </div>
      <div className="text-secondary-400 shrink-0">→</div>
    </Link>
  )
}

function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

// Icons
function GridIcon() {
  return (
    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zm10 0a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zm10 0a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"
      />
    </svg>
  )
}

function ListIcon() {
  return (
    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M4 6h16M4 10h16M4 14h16M4 18h16"
      />
    </svg>
  )
}
