import type {
  QaPackage,
  Scenario,
  TestRun,
  PaginatedResponse,
  ScenarioResult,
  TestRunSummary,
} from '@/api/types'

// Mock QA Packages
export const mockPackage: QaPackage = {
  id: 'pkg-1',
  name: 'Test API Package',
  description: 'A test package for unit testing',
  openApiSpec: '{"openapi": "3.0.0"}',
  baseUrl: 'https://api.example.com',
  status: 'READY',
  createdAt: '2026-01-15T10:00:00Z',
  updatedAt: '2026-01-15T10:00:00Z',
}

export const mockPackages: QaPackage[] = [
  mockPackage,
  {
    id: 'pkg-2',
    name: 'Second Package',
    description: 'Another test package',
    openApiSpec: '{"openapi": "3.0.0"}',
    baseUrl: 'https://api2.example.com',
    status: 'DRAFT',
    createdAt: '2026-01-14T10:00:00Z',
    updatedAt: '2026-01-14T10:00:00Z',
  },
  {
    id: 'pkg-3',
    name: 'Running Package',
    description: null,
    openApiSpec: '{"openapi": "3.0.0"}',
    baseUrl: 'https://api3.example.com',
    status: 'RUNNING',
    createdAt: '2026-01-13T10:00:00Z',
    updatedAt: '2026-01-13T10:00:00Z',
  },
]

export const mockPackagesPage: PaginatedResponse<QaPackage> = {
  content: mockPackages,
  page: 0,
  size: 12,
  totalElements: 3,
  totalPages: 1,
  first: true,
  last: true,
}

// Mock Scenarios
export const mockScenario: Scenario = {
  id: 'scenario-1',
  packageId: 'pkg-1',
  name: 'Create User Flow',
  description: 'Test creating a new user',
  steps: [
    {
      id: 'step-1',
      order: 1,
      method: 'POST',
      endpoint: '/users',
      headers: { 'Content-Type': 'application/json' },
      body: '{"name": "Test User"}',
      expectedStatus: 201,
      assertions: [],
      extractors: [],
      timeoutMs: 5000,
    },
  ],
  status: 'PASSED',
  createdAt: '2026-01-15T10:00:00Z',
  updatedAt: '2026-01-15T10:00:00Z',
}

export const mockScenarios: Scenario[] = [
  mockScenario,
  {
    id: 'scenario-2',
    packageId: 'pkg-1',
    name: 'Get Users List',
    description: 'Test listing users',
    steps: [],
    status: 'PENDING',
    createdAt: '2026-01-15T10:00:00Z',
    updatedAt: '2026-01-15T10:00:00Z',
  },
]

export const mockScenariosPage: PaginatedResponse<Scenario> = {
  content: mockScenarios,
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
  first: true,
  last: true,
}

// Mock Test Runs
export const mockTestRunSummary: TestRunSummary = {
  totalScenarios: 2,
  passedScenarios: 1,
  failedScenarios: 1,
  skippedScenarios: 0,
  duration: 1500,
}

export const mockScenarioResult: ScenarioResult = {
  scenarioId: 'scenario-1',
  scenarioName: 'Create User Flow',
  status: 'PASSED',
  stepResults: [
    {
      stepId: 'step-1',
      status: 'PASSED',
      actualStatus: 201,
      responseBody: '{"id": "user-1", "name": "Test User"}',
      responseHeaders: { 'content-type': 'application/json' },
      extractedValues: {},
      assertionResults: [],
      duration: 150,
      error: null,
    },
  ],
  duration: 150,
  error: null,
}

export const mockTestRun: TestRun = {
  id: 'run-1',
  packageId: 'pkg-1',
  status: 'COMPLETED',
  startedAt: '2026-01-15T11:00:00Z',
  completedAt: '2026-01-15T11:00:15Z',
  scenarioResults: [
    mockScenarioResult,
    {
      scenarioId: 'scenario-2',
      scenarioName: 'Get Users List',
      status: 'FAILED',
      stepResults: [],
      duration: 100,
      error: 'Connection timeout',
    },
  ],
  summary: mockTestRunSummary,
}

export const mockTestRuns: TestRun[] = [mockTestRun]

export const mockTestRunsPage: PaginatedResponse<TestRun> = {
  content: mockTestRuns,
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  first: true,
  last: true,
}
