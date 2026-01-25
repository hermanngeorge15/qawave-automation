import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_app/packages/$packageId')({
  component: PackageDetailPage,
  pendingComponent: PackageDetailLoading,
  errorComponent: PackageDetailError,
})

function PackageDetailPage() {
  const { packageId } = Route.useParams()

  return (
    <div className="package-detail-page">
      <h1>Package Details</h1>
      <p>Viewing package: {packageId}</p>
    </div>
  )
}

function PackageDetailLoading() {
  return (
    <div className="package-detail-loading">
      <p>Loading package details...</p>
    </div>
  )
}

function PackageDetailError({ error }: { error: Error }) {
  return (
    <div className="package-detail-error">
      <h2>Error loading package</h2>
      <p>{error.message}</p>
    </div>
  )
}
