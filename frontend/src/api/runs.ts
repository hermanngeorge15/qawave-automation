import apiClient from './client'
import type { TestRun, PaginatedResponse, TestRunStatus } from './types'

const BASE_PATH = '/api/qa/runs'

export interface ListRunsParams {
  page?: number | undefined
  size?: number | undefined
  packageId?: string | undefined
  status?: TestRunStatus | undefined
}

export const runsApi = {
  /**
   * List all test runs with optional filters
   */
  list(params: ListRunsParams = {}, signal?: AbortSignal): Promise<PaginatedResponse<TestRun>> {
    const searchParams = new URLSearchParams()
    searchParams.set('page', String(params.page ?? 0))
    searchParams.set('size', String(params.size ?? 20))
    if (params.packageId) searchParams.set('packageId', params.packageId)
    if (params.status) searchParams.set('status', params.status)
    return apiClient.get<PaginatedResponse<TestRun>>(`${BASE_PATH}?${searchParams.toString()}`, { signal })
  },

  /**
   * Get a single test run by ID
   */
  get(id: string, signal?: AbortSignal): Promise<TestRun> {
    return apiClient.get<TestRun>(`${BASE_PATH}/${id}`, { signal })
  },

  /**
   * Cancel a running test run
   */
  cancel(id: string): Promise<undefined> {
    return apiClient.post<undefined>(`${BASE_PATH}/${id}/cancel`)
  },

  /**
   * Retry a failed test run
   */
  retry(id: string): Promise<TestRun> {
    return apiClient.post<TestRun>(`${BASE_PATH}/${id}/retry`)
  },
}

export default runsApi
