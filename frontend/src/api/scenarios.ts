import apiClient from './client'
import type { Scenario, PaginatedResponse, TestStep } from './types'

const BASE_PATH = '/api/qa/scenarios'

export interface ScenarioValidationResult {
  valid: boolean
  errors: ScenarioValidationError[]
  warnings: ScenarioValidationError[]
}

export interface ScenarioValidationError {
  line: number
  column: number
  message: string
  path?: string
}

export interface ScenariosListParams {
  page?: number | undefined
  size?: number | undefined
  packageId?: string | undefined
  status?: string | undefined
  search?: string | undefined
}

export const scenariosApi = {
  /**
   * List all scenarios with optional filters
   */
  list(params: ScenariosListParams = {}, signal?: AbortSignal): Promise<PaginatedResponse<Scenario>> {
    const searchParams = new URLSearchParams()
    if (params.page !== undefined) searchParams.set('page', String(params.page))
    if (params.size !== undefined) searchParams.set('size', String(params.size))
    if (params.packageId) searchParams.set('packageId', params.packageId)
    if (params.status) searchParams.set('status', params.status)
    if (params.search) searchParams.set('search', params.search)

    const queryString = searchParams.toString()
    return apiClient.get<PaginatedResponse<Scenario>>(
      `${BASE_PATH}${queryString ? `?${queryString}` : ''}`,
      { signal }
    )
  },

  /**
   * List scenarios for a package
   */
  listByPackage(
    packageId: string,
    page = 0,
    size = 20,
    signal?: AbortSignal
  ): Promise<PaginatedResponse<Scenario>> {
    return apiClient.get<PaginatedResponse<Scenario>>(
      `${BASE_PATH}?packageId=${packageId}&page=${String(page)}&size=${String(size)}`,
      { signal }
    )
  },

  /**
   * Get a single scenario by ID
   */
  get(id: string, signal?: AbortSignal): Promise<Scenario> {
    return apiClient.get<Scenario>(`${BASE_PATH}/${id}`, { signal })
  },

  /**
   * Update a scenario
   */
  update(id: string, data: Partial<Scenario>): Promise<Scenario> {
    return apiClient.patch<Scenario>(`${BASE_PATH}/${id}`, data)
  },

  /**
   * Delete a scenario
   */
  delete(id: string): Promise<undefined> {
    return apiClient.delete<undefined>(`${BASE_PATH}/${id}`)
  },

  /**
   * Run a single scenario
   */
  run(id: string): Promise<undefined> {
    return apiClient.post<undefined>(`${BASE_PATH}/${id}/run`)
  },

  /**
   * Update scenario steps
   */
  updateSteps(id: string, steps: TestStep[]): Promise<Scenario> {
    return apiClient.patch<Scenario>(`${BASE_PATH}/${id}`, { steps })
  },

  /**
   * Validate scenario JSON without saving
   */
  validate(json: string): Promise<ScenarioValidationResult> {
    return apiClient.post<ScenarioValidationResult>(`${BASE_PATH}/validate`, { json })
  },
}

export default scenariosApi
