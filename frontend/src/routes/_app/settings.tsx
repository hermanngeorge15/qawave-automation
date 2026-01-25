import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_app/settings')({
  component: SettingsPage,
  pendingComponent: SettingsLoading,
  errorComponent: SettingsError,
})

function SettingsPage() {
  return (
    <div className="settings-page">
      <h1>Settings</h1>
      <p>Configure your QAWave settings here.</p>
    </div>
  )
}

function SettingsLoading() {
  return (
    <div className="settings-loading">
      <p>Loading settings...</p>
    </div>
  )
}

function SettingsError({ error }: { error: Error }) {
  return (
    <div className="settings-error">
      <h2>Error loading settings</h2>
      <p>{error.message}</p>
    </div>
  )
}
