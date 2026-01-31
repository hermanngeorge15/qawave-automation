import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { runsApi } from '@/api'
import type { ListRunsParams } from '@/api/runs'

// Query key factory
export const runKeys = {
  all: ['runs'] as const,
  lists: () => [...runKeys.all, 'list'] as const,
  list: (params: ListRunsParams) => [...runKeys.lists(), params] as const,
  details: () => [...runKeys.all, 'detail'] as const,
  detail: (id: string) => [...runKeys.details(), id] as const,
}

// Hooks
export function useRuns(params: ListRunsParams = {}) {
  return useQuery({
    queryKey: runKeys.list(params),
    queryFn: ({ signal }) => runsApi.list(params, signal),
  })
}

export function useRun(id: string, options?: { refetchInterval?: number | false }) {
  return useQuery({
    queryKey: runKeys.detail(id),
    queryFn: ({ signal }) => runsApi.get(id, signal),
    enabled: Boolean(id),
    ...(options?.refetchInterval !== undefined && { refetchInterval: options.refetchInterval }),
  })
}

export function useCancelRun() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => runsApi.cancel(id),
    onSuccess: (_data, id) => {
      void queryClient.invalidateQueries({ queryKey: runKeys.detail(id) })
    },
  })
}

export function useRetryRun() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => runsApi.retry(id),
    onSuccess: (newRun, _oldId) => {
      void queryClient.invalidateQueries({ queryKey: runKeys.all })
      void queryClient.setQueryData(runKeys.detail(newRun.id), newRun)
    },
  })
}
