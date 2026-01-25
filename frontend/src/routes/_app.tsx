import { createFileRoute, Outlet } from '@tanstack/react-router'
import { MainLayout } from '@/components/layouts'

export const Route = createFileRoute('/_app')({
  component: AppLayout,
})

function AppLayout() {
  return (
    <MainLayout>
      <Outlet />
    </MainLayout>
  )
}
