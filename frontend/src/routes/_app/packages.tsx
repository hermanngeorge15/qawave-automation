import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_app/packages')({
  component: PackagesPage,
  pendingComponent: PackagesLoading,
  errorComponent: PackagesError,
})

function PackagesPage() {
  return (
    <div className="packages-page">
      <h1>QA Packages</h1>
      <p>Manage your QA automation packages here.</p>
    </div>
  )
}

function PackagesLoading() {
  return (
    <div className="packages-loading">
      <p>Loading packages...</p>
    </div>
  )
}

function PackagesError({ error }: { error: Error }) {
  return (
    <div className="packages-error">
      <h2>Error loading packages</h2>
      <p>{error.message}</p>
    </div>
  )
}
