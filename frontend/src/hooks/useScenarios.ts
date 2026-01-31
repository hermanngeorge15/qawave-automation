import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { scenariosApi, type ScenariosListParams } from '@/api/scenarios'
import { packageKeys } from './usePackages'
import type { TestStep } from '@/api/types'

// Query key factory
export const scenarioKeys = {
  all: ['scenarios'] as const,
  lists: () => [...scenarioKeys.all, 'list'] as const,
  list: (params: ScenariosListParams) => [...scenarioKeys.lists(), params] as const,
  listByPackage: (packageId: string, page: number, size: number) =>
    [...scenarioKeys.lists(), { packageId, page, size }] as const,
  details: () => [...scenarioKeys.all, 'detail'] as const,
  detail: (id: string) => [...scenarioKeys.details(), id] as const,
}

// Hooks
export function useScenarios(params: ScenariosListParams = {}) {
  return useQuery({
    queryKey: scenarioKeys.list(params),
    queryFn: ({ signal }) => scenariosApi.list(params, signal),
  })
}

export function usePackageScenarios(packageId: string, page = 0, size = 20) {
  return useQuery({
    queryKey: scenarioKeys.listByPackage(packageId, page, size),
    queryFn: ({ signal }) => scenariosApi.listByPackage(packageId, page, size, signal),
    enabled: Boolean(packageId),
  })
}

export function useScenario(id: string) {
  return useQuery({
    queryKey: scenarioKeys.detail(id),
    queryFn: ({ signal }) => scenariosApi.get(id, signal),
    enabled: Boolean(id),
  })
}

export function useRunScenario() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => scenariosApi.run(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: scenarioKeys.all })
      void queryClient.invalidateQueries({ queryKey: packageKeys.all })
    },
  })
}

export function useDeleteScenario() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => scenariosApi.delete(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: scenarioKeys.lists() })
      void queryClient.invalidateQueries({ queryKey: packageKeys.all })
    },
  })
}

export function useUpdateScenarioSteps(scenarioId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (steps: TestStep[]) => scenariosApi.updateSteps(scenarioId, steps),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: scenarioKeys.detail(scenarioId) })
      void queryClient.invalidateQueries({ queryKey: scenarioKeys.lists() })
    },
  })
}

export function useValidateScenario() {
  return useMutation({
    mutationFn: (json: string) => scenariosApi.validate(json),
  })
}
