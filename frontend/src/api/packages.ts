import apiClient from './client'
import type {
  QaPackage,
  CreateQaPackageRequest,
  UpdateQaPackageRequest,
  PaginatedResponse,
  TestRun,
} from './types'

const BASE_PATH = '/api/qa/packages'

export const packagesApi = {
  /**
   * List all QA packages with pagination
   */
  list(page = 0, size = 20, signal?: AbortSignal): Promise<PaginatedResponse<QaPackage>> {
    return apiClient.get<PaginatedResponse<QaPackage>>(
      `${BASE_PATH}?page=${String(page)}&size=${String(size)}`,
      { signal }
    )
  },

  /**
   * Get a single QA package by ID
   */
  get(id: string, signal?: AbortSignal): Promise<QaPackage> {
    return apiClient.get<QaPackage>(`${BASE_PATH}/${id}`, { signal })
  },

  /**
   * Create a new QA package
   */
  create(data: CreateQaPackageRequest): Promise<QaPackage> {
    return apiClient.post<QaPackage>(BASE_PATH, data)
  },

  /**
   * Update an existing QA package
   */
  update(id: string, data: UpdateQaPackageRequest): Promise<QaPackage> {
    return apiClient.patch<QaPackage>(`${BASE_PATH}/${id}`, data)
  },

  /**
   * Delete a QA package
   */
  delete(id: string): Promise<undefined> {
    return apiClient.delete<undefined>(`${BASE_PATH}/${id}`)
  },

  /**
   * Trigger scenario generation for a package
   */
  generateScenarios(id: string): Promise<undefined> {
    return apiClient.post<undefined>(`${BASE_PATH}/${id}/generate`)
  },

  /**
   * Start a test run for a package
   */
  startRun(id: string): Promise<TestRun> {
    return apiClient.post<TestRun>(`${BASE_PATH}/${id}/runs`)
  },

  /**
   * List all test runs for a package
   */
  listRuns(id: string, page = 0, size = 20, signal?: AbortSignal): Promise<PaginatedResponse<TestRun>> {
    return apiClient.get<PaginatedResponse<TestRun>>(
      `${BASE_PATH}/${id}/runs?page=${String(page)}&size=${String(size)}`,
      { signal }
    )
  },
}

export default packagesApi
