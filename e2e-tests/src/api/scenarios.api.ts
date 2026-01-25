import { APIRequestContext } from '@playwright/test';
import { ApiClient, ApiResponse } from './client';
import { PagedResponse } from './packages.api';

/**
 * Scenario entity type
 */
export interface Scenario {
  id: string;
  packageId: string;
  name: string;
  description?: string;
  steps: ScenarioStep[];
  createdAt: string;
  updatedAt: string;
}

/**
 * Scenario step
 */
export interface ScenarioStep {
  id: string;
  name: string;
  method: string;
  path: string;
  expectedStatus: number;
  requestBody?: Record<string, unknown>;
  assertions?: StepAssertion[];
}

/**
 * Step assertion
 */
export interface StepAssertion {
  type: 'status' | 'jsonPath' | 'header';
  path?: string;
  expected: unknown;
}

/**
 * Create scenario request
 */
export interface CreateScenarioRequest {
  name: string;
  description?: string;
  steps?: ScenarioStep[];
}

/**
 * Scenarios API client
 */
export class ScenariosApi extends ApiClient {
  private readonly basePath = '/api/qa/scenarios';

  constructor(request: APIRequestContext) {
    super(request);
  }

  /**
   * List all scenarios
   */
  async list(page = 0, size = 20): Promise<ApiResponse<PagedResponse<Scenario>>> {
    return this.get<PagedResponse<Scenario>>(this.basePath, {
      params: { page: page.toString(), size: size.toString() },
    });
  }

  /**
   * List scenarios for a specific package
   */
  async listByPackage(
    packageId: string,
    page = 0,
    size = 20
  ): Promise<ApiResponse<PagedResponse<Scenario>>> {
    return this.get<PagedResponse<Scenario>>(this.basePath, {
      params: {
        packageId,
        page: page.toString(),
        size: size.toString(),
      },
    });
  }

  /**
   * Get scenario by ID
   */
  async getById(id: string): Promise<ApiResponse<Scenario>> {
    return this.get<Scenario>(`${this.basePath}/${id}`);
  }

  /**
   * Create new scenario
   */
  async create(
    packageId: string,
    data: CreateScenarioRequest
  ): Promise<ApiResponse<Scenario>> {
    return this.post<Scenario>(`/api/qa/packages/${packageId}/scenarios`, data);
  }

  /**
   * Update scenario
   */
  async update(
    id: string,
    data: Partial<CreateScenarioRequest>
  ): Promise<ApiResponse<Scenario>> {
    return this.put<Scenario>(`${this.basePath}/${id}`, data);
  }

  /**
   * Delete scenario
   */
  async deleteScenario(id: string): Promise<ApiResponse<void>> {
    return this.delete(`${this.basePath}/${id}`);
  }

  /**
   * Generate scenarios from OpenAPI spec
   */
  async generate(packageId: string): Promise<ApiResponse<{ count: number }>> {
    return this.post<{ count: number }>(
      `/api/qa/packages/${packageId}/scenarios/generate`
    );
  }
}
