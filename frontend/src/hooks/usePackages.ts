import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { packagesApi } from '@/api'
import type { CreateQaPackageRequest, UpdateQaPackageRequest } from '@/api/types'

// Query key factory
export const packageKeys = {
  all: ['packages'] as const,
  lists: () => [...packageKeys.all, 'list'] as const,
  list: (page: number, size: number) => [...packageKeys.lists(), { page, size }] as const,
  details: () => [...packageKeys.all, 'detail'] as const,
  detail: (id: string) => [...packageKeys.details(), id] as const,
  runs: (id: string) => [...packageKeys.detail(id), 'runs'] as const,
}

// Hooks
export function usePackages(page = 0, size = 20) {
  return useQuery({
    queryKey: packageKeys.list(page, size),
    queryFn: ({ signal }) => packagesApi.list(page, size, signal),
  })
}

export function usePackage(id: string) {
  return useQuery({
    queryKey: packageKeys.detail(id),
    queryFn: ({ signal }) => packagesApi.get(id, signal),
    enabled: Boolean(id),
  })
}

export function useCreatePackage() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateQaPackageRequest) => packagesApi.create(data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: packageKeys.lists() })
    },
  })
}

export function useDeletePackage() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => packagesApi.delete(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: packageKeys.lists() })
    },
  })
}

export function useUpdatePackage() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateQaPackageRequest }) =>
      packagesApi.update(id, data),
    onSuccess: (_data, { id }) => {
      void queryClient.invalidateQueries({ queryKey: packageKeys.detail(id) })
      void queryClient.invalidateQueries({ queryKey: packageKeys.lists() })
    },
  })
}

export function useGenerateScenarios() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => packagesApi.generateScenarios(id),
    onSuccess: (_data, id) => {
      void queryClient.invalidateQueries({ queryKey: packageKeys.detail(id) })
    },
  })
}

export function useStartRun() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => packagesApi.startRun(id),
    onSuccess: (_data, id) => {
      void queryClient.invalidateQueries({ queryKey: packageKeys.detail(id) })
      void queryClient.invalidateQueries({ queryKey: packageKeys.runs(id) })
    },
  })
}

export function usePackageRuns(id: string, page = 0, size = 20) {
  return useQuery({
    queryKey: [...packageKeys.runs(id), { page, size }] as const,
    queryFn: ({ signal }) => packagesApi.listRuns(id, page, size, signal),
    enabled: Boolean(id),
  })
}
