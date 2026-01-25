import apiClient from './client'
import type { Scenario, PaginatedResponse } from './types'

const BASE_PATH = '/api/qa/scenarios'

export const scenariosApi = {
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
}

export default scenariosApi
