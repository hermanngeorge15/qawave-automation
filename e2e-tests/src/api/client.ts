import { APIRequestContext, APIResponse } from '@playwright/test';

const API_URL = process.env.API_URL ?? 'http://localhost:8080';

/**
 * API response wrapper with typed body
 */
export interface ApiResponse<T> {
  status: number;
  body: T;
  headers: Record<string, string>;
  ok: boolean;
}

/**
 * Base API client for backend communication
 */
export class ApiClient {
  constructor(protected request: APIRequestContext) {}

  /**
   * Make GET request
   */
  async get<T>(path: string, options?: { params?: Record<string, string> }): Promise<ApiResponse<T>> {
    const url = new URL(path, API_URL);
    if (options?.params) {
      Object.entries(options.params).forEach(([key, value]) => {
        url.searchParams.append(key, value);
      });
    }

    const response = await this.request.get(url.toString());
    return this.parseResponse<T>(response);
  }

  /**
   * Make POST request
   */
  async post<T>(path: string, data?: unknown): Promise<ApiResponse<T>> {
    const response = await this.request.post(`${API_URL}${path}`, {
      data,
    });
    return this.parseResponse<T>(response);
  }

  /**
   * Make PUT request
   */
  async put<T>(path: string, data?: unknown): Promise<ApiResponse<T>> {
    const response = await this.request.put(`${API_URL}${path}`, {
      data,
    });
    return this.parseResponse<T>(response);
  }

  /**
   * Make PATCH request
   */
  async patch<T>(path: string, data?: unknown): Promise<ApiResponse<T>> {
    const response = await this.request.patch(`${API_URL}${path}`, {
      data,
    });
    return this.parseResponse<T>(response);
  }

  /**
   * Make DELETE request
   */
  async delete<T = void>(path: string): Promise<ApiResponse<T>> {
    const response = await this.request.delete(`${API_URL}${path}`);
    return this.parseResponse<T>(response);
  }

  /**
   * Parse API response
   */
  private async parseResponse<T>(response: APIResponse): Promise<ApiResponse<T>> {
    let body: T;
    const contentType = response.headers()['content-type'] ?? '';

    if (contentType.includes('application/json')) {
      try {
        body = await response.json();
      } catch {
        body = {} as T;
      }
    } else {
      body = (await response.text()) as unknown as T;
    }

    const headers: Record<string, string> = {};
    Object.entries(response.headers()).forEach(([key, value]) => {
      headers[key] = value;
    });

    return {
      status: response.status(),
      body,
      headers,
      ok: response.ok(),
    };
  }
}
