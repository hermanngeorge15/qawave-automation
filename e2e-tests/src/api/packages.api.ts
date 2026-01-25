import { APIRequestContext } from '@playwright/test';
import { ApiClient, ApiResponse } from './client';

/**
 * Package entity type
 */
export interface Package {
  id: string;
  name: string;
  specUrl: string;
  baseUrl?: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * Create package request
 */
export interface CreatePackageRequest {
  name?: string;
  specUrl: string;
  baseUrl?: string;
  description?: string;
}

/**
 * Paginated response
 */
export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

/**
 * Packages API client
 */
export class PackagesApi extends ApiClient {
  private readonly basePath = '/api/qa/packages';

  constructor(request: APIRequestContext) {
    super(request);
  }

  /**
   * List all packages with pagination
   */
  async list(page = 0, size = 20): Promise<ApiResponse<PagedResponse<Package>>> {
    return this.get<PagedResponse<Package>>(this.basePath, {
      params: { page: page.toString(), size: size.toString() },
    });
  }

  /**
   * Get package by ID
   */
  async getById(id: string): Promise<ApiResponse<Package>> {
    return this.get<Package>(`${this.basePath}/${id}`);
  }

  /**
   * Create new package
   */
  async create(data: CreatePackageRequest): Promise<ApiResponse<Package>> {
    return this.post<Package>(this.basePath, data);
  }

  /**
   * Update package
   */
  async update(id: string, data: Partial<CreatePackageRequest>): Promise<ApiResponse<Package>> {
    return this.put<Package>(`${this.basePath}/${id}`, data);
  }

  /**
   * Delete package
   */
  async deletePackage(id: string): Promise<ApiResponse<void>> {
    return this.delete(`${this.basePath}/${id}`);
  }

  /**
   * Trigger test run for package
   */
  async triggerRun(packageId: string): Promise<ApiResponse<{ runId: string }>> {
    return this.post<{ runId: string }>(`${this.basePath}/${packageId}/runs`);
  }
}
