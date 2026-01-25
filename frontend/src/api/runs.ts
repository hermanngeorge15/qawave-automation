import apiClient from './client'
import type { TestRun } from './types'

const BASE_PATH = '/api/qa/runs'

export const runsApi = {
  /**
   * Get a single test run by ID
   */
  get(id: string, signal?: AbortSignal): Promise<TestRun> {
    return apiClient.get<TestRun>(`${BASE_PATH}/${id}`, { signal })
  },

  /**
   * Cancel a running test run
   */
  cancel(id: string): Promise<void> {
    return apiClient.post<void>(`${BASE_PATH}/${id}/cancel`)
  },

  /**
   * Retry a failed test run
   */
  retry(id: string): Promise<TestRun> {
    return apiClient.post<TestRun>(`${BASE_PATH}/${id}/retry`)
  },
}

export default runsApi
