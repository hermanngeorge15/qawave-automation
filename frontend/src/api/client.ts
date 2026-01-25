import type { ApiErrorResponse } from './types'

// Configuration
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080'
const DEFAULT_TIMEOUT_MS = 30000

// Custom Error Classes
export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code: string,
    public readonly details?: Record<string, unknown> | undefined
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export class NetworkError extends Error {
  constructor(
    message: string,
    public readonly cause?: unknown
  ) {
    super(message)
    this.name = 'NetworkError'
  }
}

export class TimeoutError extends Error {
  constructor(message = 'Request timed out') {
    super(message)
    this.name = 'TimeoutError'
  }
}

// Request interceptors
type RequestInterceptor = (config: RequestInit) => RequestInit | Promise<RequestInit>
type ResponseInterceptor = (response: Response) => Response | Promise<Response>

const requestInterceptors: RequestInterceptor[] = []
const responseInterceptors: ResponseInterceptor[] = []

export function addRequestInterceptor(interceptor: RequestInterceptor): () => void {
  requestInterceptors.push(interceptor)
  return () => {
    const index = requestInterceptors.indexOf(interceptor)
    if (index > -1) {
      requestInterceptors.splice(index, 1)
    }
  }
}

export function addResponseInterceptor(interceptor: ResponseInterceptor): () => void {
  responseInterceptors.push(interceptor)
  return () => {
    const index = responseInterceptors.indexOf(interceptor)
    if (index > -1) {
      responseInterceptors.splice(index, 1)
    }
  }
}

// Auth token management
let authToken: string | null = null

export function setAuthToken(token: string | null): void {
  authToken = token
}

export function getAuthToken(): string | null {
  return authToken
}

// Default request interceptor for auth
addRequestInterceptor((config) => {
  if (authToken) {
    const headers = new Headers(config.headers)
    headers.set('Authorization', `Bearer ${authToken}`)
    return { ...config, headers }
  }
  return config
})

// Request options
interface RequestOptions {
  method?: string
  headers?: HeadersInit
  body?: unknown
  timeout?: number
  signal?: AbortSignal | undefined
}

// Main fetch function
async function apiFetch<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const { body, timeout = DEFAULT_TIMEOUT_MS, signal: externalSignal, method, headers } = options

  // Create abort controller for timeout
  const timeoutController = new AbortController()
  const timeoutId = setTimeout(() => {
    timeoutController.abort()
  }, timeout)

  // Combine signals if external signal provided
  const signal = externalSignal
    ? combineAbortSignals(externalSignal, timeoutController.signal)
    : timeoutController.signal

  const url = `${API_BASE_URL}${endpoint}`

  // Build request config
  let config: RequestInit = {
    method: method ?? 'GET',
    signal,
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...(headers as Record<string, string> | undefined),
    },
    body: body !== undefined ? JSON.stringify(body) : null,
  }

  // Apply request interceptors
  for (const interceptor of requestInterceptors) {
    config = await interceptor(config)
  }

  try {
    let response = await fetch(url, config)

    // Apply response interceptors
    for (const interceptor of responseInterceptors) {
      response = await interceptor(response)
    }

    if (!response.ok) {
      const errorData = await parseErrorResponse(response)
      throw new ApiError(errorData.message, response.status, errorData.code, errorData.details)
    }

    // Handle empty responses
    if (response.status === 204) {
      return undefined as T
    }

    const data: unknown = await response.json()
    return data as T
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }

    if (error instanceof DOMException && error.name === 'AbortError') {
      if (timeoutController.signal.aborted) {
        throw new TimeoutError()
      }
      throw error // Re-throw if aborted externally
    }

    throw new NetworkError(
      error instanceof Error ? error.message : 'Network request failed',
      error
    )
  } finally {
    clearTimeout(timeoutId)
  }
}

async function parseErrorResponse(response: Response): Promise<ApiErrorResponse> {
  try {
    const data = (await response.json()) as {
      message?: string
      code?: string
      details?: Record<string, unknown>
      timestamp?: string
    }
    return {
      message: data.message ?? 'An error occurred',
      code: data.code ?? 'UNKNOWN_ERROR',
      ...(data.details && { details: data.details }),
      timestamp: data.timestamp ?? new Date().toISOString(),
    }
  } catch {
    return {
      message: response.statusText || 'An error occurred',
      code: 'PARSE_ERROR',
      timestamp: new Date().toISOString(),
    }
  }
}

function combineAbortSignals(...signals: AbortSignal[]): AbortSignal {
  const controller = new AbortController()

  for (const signal of signals) {
    if (signal.aborted) {
      controller.abort()
      return controller.signal
    }

    signal.addEventListener('abort', () => {
      controller.abort()
    }, { once: true })
  }

  return controller.signal
}

// HTTP method helpers
export const apiClient = {
  get<T>(endpoint: string, options?: RequestOptions): Promise<T> {
    return apiFetch<T>(endpoint, { ...options, method: 'GET' })
  },

  post<T>(endpoint: string, body?: unknown, options?: RequestOptions): Promise<T> {
    return apiFetch<T>(endpoint, { ...options, method: 'POST', body })
  },

  put<T>(endpoint: string, body?: unknown, options?: RequestOptions): Promise<T> {
    return apiFetch<T>(endpoint, { ...options, method: 'PUT', body })
  },

  patch<T>(endpoint: string, body?: unknown, options?: RequestOptions): Promise<T> {
    return apiFetch<T>(endpoint, { ...options, method: 'PATCH', body })
  },

  delete<T>(endpoint: string, options?: RequestOptions): Promise<T> {
    return apiFetch<T>(endpoint, { ...options, method: 'DELETE' })
  },
}

export default apiClient
