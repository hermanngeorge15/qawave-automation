// API Response Types

export interface PaginatedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

// QA Package Types
export interface QaPackage {
  id: string
  name: string
  description: string | null
  openApiSpec: string
  baseUrl: string
  status: QaPackageStatus
  createdAt: string
  updatedAt: string
}

export type QaPackageStatus = 'DRAFT' | 'GENERATING' | 'READY' | 'RUNNING' | 'COMPLETED' | 'FAILED'

export interface CreateQaPackageRequest {
  name: string
  description?: string
  openApiSpec: string
  baseUrl: string
}

export interface UpdateQaPackageRequest {
  name?: string
  description?: string
  openApiSpec?: string
  baseUrl?: string
}

// Scenario Types
export interface Scenario {
  id: string
  packageId: string
  name: string
  description: string | null
  steps: TestStep[]
  status: ScenarioStatus
  createdAt: string
  updatedAt: string
}

export type ScenarioStatus = 'PENDING' | 'RUNNING' | 'PASSED' | 'FAILED' | 'SKIPPED'

export interface TestStep {
  id: string
  order: number
  method: HttpMethod
  endpoint: string
  headers: Record<string, string>
  body: string | null
  expectedStatus: number
  assertions: StepAssertion[]
  extractors: StepExtractor[]
  timeoutMs: number
}

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

export interface StepAssertion {
  type: AssertionType
  path: string
  expected: unknown
  operator: ComparisonOperator
}

export type AssertionType = 'STATUS' | 'JSON_PATH' | 'HEADER' | 'BODY_CONTAINS'

export type ComparisonOperator = 'EQUALS' | 'NOT_EQUALS' | 'CONTAINS' | 'GREATER_THAN' | 'LESS_THAN'

export interface StepExtractor {
  name: string
  type: ExtractorType
  path: string
}

export type ExtractorType = 'JSON_PATH' | 'HEADER' | 'REGEX'

// Test Run Types
export interface TestRun {
  id: string
  packageId: string
  status: TestRunStatus
  startedAt: string | null
  completedAt: string | null
  scenarioResults: ScenarioResult[]
  summary: TestRunSummary
}

export type TestRunStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'CANCELLED'

export interface TestRunSummary {
  totalScenarios: number
  passedScenarios: number
  failedScenarios: number
  skippedScenarios: number
  duration: number
}

export interface ScenarioResult {
  scenarioId: string
  scenarioName: string
  status: ScenarioStatus
  stepResults: StepResult[]
  duration: number
  error: string | null
}

export interface StepResult {
  stepId: string
  status: StepResultStatus
  actualStatus: number
  responseBody: string | null
  responseHeaders: Record<string, string>
  extractedValues: Record<string, unknown>
  assertionResults: AssertionResult[]
  duration: number
  error: string | null
}

export type StepResultStatus = 'PASSED' | 'FAILED' | 'SKIPPED' | 'ERROR'

export interface AssertionResult {
  assertion: StepAssertion
  passed: boolean
  actualValue: unknown
  message: string | null
}

// Error Response
export interface ApiErrorResponse {
  message: string
  code: string
  details?: Record<string, unknown>
  timestamp: string
}
