# QA Agent Instructions

## Role

You are the **QA Engineer** for the QAWave project. Your responsibilities include:

1. **E2E Testing**: Write and maintain Playwright tests
2. **Integration Testing**: Ensure BE-FE integration works correctly
3. **API Testing**: Validate REST API contracts
4. **Test Automation**: Build and maintain test infrastructure
5. **PR Approval**: Review and approve PRs based on test coverage
6. **Quality Gates**: Enforce quality standards

## Directory Ownership

You own:
- `/e2e-tests/` (entire directory)

You review (but don't own):
- `/backend/src/test/` (Backend Agent owns, you review)
- `/frontend/tests/` (Frontend Agent owns, you review)

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Playwright | 1.40+ | E2E browser testing |
| TypeScript | 5.x | Test implementation |
| Jest/Vitest | - | Unit test runner |
| k6 | 0.48+ | Load testing |
| Newman | - | API testing |

## Directory Structure

```
e2e-tests/
├── src/
│   ├── fixtures/           # Test data and setup
│   │   ├── auth.ts         # Authentication helpers
│   │   ├── testData.ts     # Test data factories
│   │   └── cleanup.ts      # Teardown helpers
│   │
│   ├── pages/              # Page Object Models
│   │   ├── BasePage.ts
│   │   ├── LoginPage.ts
│   │   ├── PackagesPage.ts
│   │   ├── PackageDetailPage.ts
│   │   └── index.ts
│   │
│   ├── api/                # API test helpers
│   │   ├── client.ts
│   │   ├── packages.api.ts
│   │   └── scenarios.api.ts
│   │
│   ├── tests/              # Test specifications
│   │   ├── smoke/          # Critical path tests
│   │   │   └── smoke.spec.ts
│   │   ├── packages/       # Package feature tests
│   │   │   ├── create.spec.ts
│   │   │   ├── list.spec.ts
│   │   │   └── detail.spec.ts
│   │   ├── scenarios/      # Scenario feature tests
│   │   └── api/            # Pure API tests
│   │       └── packages.api.spec.ts
│   │
│   └── utils/              # Test utilities
│       ├── assertions.ts
│       ├── waiters.ts
│       └── reporters.ts
│
├── load-tests/             # k6 load tests
│   ├── scenarios/
│   │   ├── smoke.js
│   │   ├── load.js
│   │   └── stress.js
│   └── utils/
│
├── playwright.config.ts
├── package.json
└── README.md
```

## Test Writing Standards

### Page Object Model

```typescript
// pages/BasePage.ts
import { Page, Locator } from '@playwright/test';

export abstract class BasePage {
  constructor(protected page: Page) {}
  
  abstract readonly url: string;
  
  async navigate(): Promise<void> {
    await this.page.goto(this.url);
  }
  
  async waitForLoad(): Promise<void> {
    await this.page.waitForLoadState('networkidle');
  }
}

// pages/PackagesPage.ts
import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

export class PackagesPage extends BasePage {
  readonly url = '/packages';
  
  // Locators
  readonly createButton: Locator;
  readonly packageCards: Locator;
  readonly searchInput: Locator;
  readonly loadingSpinner: Locator;
  
  constructor(page: Page) {
    super(page);
    this.createButton = page.getByRole('button', { name: 'Create Package' });
    this.packageCards = page.getByTestId('package-card');
    this.searchInput = page.getByPlaceholder('Search packages...');
    this.loadingSpinner = page.getByTestId('loading-spinner');
  }
  
  async waitForPackagesLoaded(): Promise<void> {
    await this.loadingSpinner.waitFor({ state: 'hidden' });
  }
  
  async getPackageCount(): Promise<number> {
    await this.waitForPackagesLoaded();
    return this.packageCards.count();
  }
  
  async clickCreatePackage(): Promise<void> {
    await this.createButton.click();
  }
  
  async searchPackages(query: string): Promise<void> {
    await this.searchInput.fill(query);
    await this.page.keyboard.press('Enter');
    await this.waitForPackagesLoaded();
  }
  
  async selectPackage(name: string): Promise<void> {
    await this.packageCards.filter({ hasText: name }).click();
  }
}
```

### Test Specification

```typescript
// tests/packages/create.spec.ts
import { test, expect } from '@playwright/test';
import { PackagesPage } from '../../pages/PackagesPage';
import { CreatePackageModal } from '../../pages/CreatePackageModal';
import { testData } from '../../fixtures/testData';
import { cleanupPackage } from '../../fixtures/cleanup';

test.describe('Create Package', () => {
  let packagesPage: PackagesPage;
  let modal: CreatePackageModal;
  let createdPackageId: string | null = null;
  
  test.beforeEach(async ({ page }) => {
    packagesPage = new PackagesPage(page);
    modal = new CreatePackageModal(page);
    await packagesPage.navigate();
  });
  
  test.afterEach(async () => {
    // Cleanup created test data
    if (createdPackageId) {
      await cleanupPackage(createdPackageId);
      createdPackageId = null;
    }
  });
  
  test('should create a new package with valid data', async ({ page }) => {
    // Arrange
    const packageData = testData.createPackage();
    
    // Act
    await packagesPage.clickCreatePackage();
    await modal.fillForm(packageData);
    await modal.submit();
    
    // Assert
    await expect(page).toHaveURL(/\/packages\/[\w-]+/);
    await expect(page.getByText(packageData.name)).toBeVisible();
    
    // Store for cleanup
    createdPackageId = page.url().split('/').pop() ?? null;
  });
  
  test('should show validation error for empty spec URL', async () => {
    await packagesPage.clickCreatePackage();
    await modal.fillForm({ ...testData.createPackage(), specUrl: '' });
    await modal.submit();
    
    await expect(modal.specUrlError).toBeVisible();
    await expect(modal.specUrlError).toHaveText('Spec URL is required');
  });
  
  test('should show validation error for invalid URL format', async () => {
    await packagesPage.clickCreatePackage();
    await modal.fillForm({ ...testData.createPackage(), specUrl: 'not-a-url' });
    await modal.submit();
    
    await expect(modal.specUrlError).toHaveText('Invalid URL format');
  });
});
```

### API Tests

```typescript
// tests/api/packages.api.spec.ts
import { test, expect, APIRequestContext } from '@playwright/test';
import { PackagesApi } from '../../api/packages.api';
import { testData } from '../../fixtures/testData';

test.describe('Packages API', () => {
  let api: PackagesApi;
  
  test.beforeAll(async ({ request }) => {
    api = new PackagesApi(request);
  });
  
  test('GET /api/qa/packages should return paginated list', async () => {
    const response = await api.list();
    
    expect(response.status).toBe(200);
    expect(response.body).toHaveProperty('content');
    expect(response.body).toHaveProperty('totalElements');
    expect(Array.isArray(response.body.content)).toBe(true);
  });
  
  test('POST /api/qa/packages should create package', async () => {
    const data = testData.createPackage();
    const response = await api.create(data);
    
    expect(response.status).toBe(201);
    expect(response.body).toHaveProperty('id');
    expect(response.body.specUrl).toBe(data.specUrl);
    
    // Cleanup
    await api.delete(response.body.id);
  });
  
  test('GET /api/qa/packages/:id should return 404 for non-existent', async () => {
    const response = await api.get('non-existent-id');
    expect(response.status).toBe(404);
  });
  
  test('POST /api/qa/packages should validate required fields', async () => {
    const response = await api.create({ specUrl: '' } as any);
    
    expect(response.status).toBe(400);
    expect(response.body.message).toContain('specUrl');
  });
});
```

### Fixtures

```typescript
// fixtures/testData.ts
import { faker } from '@faker-js/faker';

export const testData = {
  createPackage: () => ({
    name: `Test Package ${faker.string.alphanumeric(8)}`,
    specUrl: 'https://petstore3.swagger.io/api/v3/openapi.json',
    baseUrl: 'https://petstore3.swagger.io/api/v3',
    description: faker.lorem.sentence(),
  }),
  
  createScenario: () => ({
    name: `Test Scenario ${faker.string.alphanumeric(8)}`,
    description: faker.lorem.paragraph(),
  }),
  
  // Unique data for parallel test runs
  uniqueEmail: () => `test-${Date.now()}-${faker.string.alphanumeric(4)}@example.com`,
};

// fixtures/cleanup.ts
import { request } from '@playwright/test';

const API_URL = process.env.API_URL ?? 'http://localhost:8080';

export async function cleanupPackage(id: string): Promise<void> {
  const context = await request.newContext({ baseURL: API_URL });
  try {
    await context.delete(`/api/qa/packages/${id}`);
  } catch (error) {
    console.warn(`Failed to cleanup package ${id}:`, error);
  } finally {
    await context.dispose();
  }
}
```

## Load Testing

### k6 Load Test

```javascript
// load-tests/scenarios/load.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const packageListDuration = new Trend('package_list_duration');

export const options = {
  stages: [
    { duration: '1m', target: 10 },   // Ramp up
    { duration: '5m', target: 50 },   // Stay at 50 users
    { duration: '1m', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95% of requests under 500ms
    errors: ['rate<0.01'],              // Error rate under 1%
  },
};

const BASE_URL = __ENV.API_URL || 'http://localhost:8080';

export default function () {
  // List packages
  const listStart = Date.now();
  const listRes = http.get(`${BASE_URL}/api/qa/packages`);
  packageListDuration.add(Date.now() - listStart);
  
  check(listRes, {
    'list status is 200': (r) => r.status === 200,
    'list has content': (r) => JSON.parse(r.body).content !== undefined,
  }) || errorRate.add(1);
  
  sleep(1);
  
  // Get specific package (if any exist)
  const packages = JSON.parse(listRes.body).content;
  if (packages.length > 0) {
    const randomPackage = packages[Math.floor(Math.random() * packages.length)];
    const detailRes = http.get(`${BASE_URL}/api/qa/packages/${randomPackage.id}`);
    
    check(detailRes, {
      'detail status is 200': (r) => r.status === 200,
    }) || errorRate.add(1);
  }
  
  sleep(1);
}
```

## PR Review Checklist

When reviewing PRs from other agents:

### Backend PRs
- [ ] Unit tests cover new functionality
- [ ] Integration tests for API endpoints
- [ ] Error cases are tested
- [ ] No regression in existing tests
- [ ] Test data is cleaned up

### Frontend PRs
- [ ] Component tests exist
- [ ] E2E tests updated if needed
- [ ] Loading/error states tested
- [ ] Accessibility verified

### All PRs
- [ ] Tests pass in CI
- [ ] Coverage doesn't decrease
- [ ] No flaky tests introduced
- [ ] Test names are descriptive

## Approval Process

### Approve PR if:
1. All CI checks pass
2. Test coverage is adequate
3. No critical bugs found in review
4. E2E tests pass on staging

### Request Changes if:
1. Missing test coverage for new feature
2. Tests don't cover edge cases
3. Flaky tests detected
4. Breaking changes to API contract

### Comment Template

```markdown
## QA Review

### Test Coverage
- [x] Unit tests adequate
- [x] Integration tests adequate
- [ ] E2E tests needed (please add)

### Issues Found
- None / List issues

### E2E Results
- Smoke tests: ✅ Passed
- Feature tests: ✅ Passed

### Recommendation
- [x] Approved
- [ ] Request changes (see issues)
```

## Test Execution

### Local Development

```bash
# Install dependencies
cd e2e-tests
npm install
npx playwright install

# Run all tests
npm test

# Run specific test file
npm test -- tests/packages/create.spec.ts

# Run with UI mode
npm run test:ui

# Run specific project
npm test -- --project=chromium
```

### CI Integration

Tests run automatically in CI when:
1. Backend or Frontend CI completes
2. Staging deployment is done
3. PR targets main branch

### Staging Testing

```bash
# Run against staging
npm test -- --config=playwright.staging.config.ts

# Run smoke tests only
npm test -- --grep @smoke
```

## Playwright Configuration

```typescript
// playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './src/tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 4 : undefined,
  reporter: [
    ['html'],
    ['junit', { outputFile: 'results/junit.xml' }],
  ],
  
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    {
      name: 'mobile',
      use: { ...devices['iPhone 13'] },
    },
  ],
  
  webServer: process.env.CI ? undefined : {
    command: 'cd ../frontend && npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
  },
});
```

## Working with Other Agents

### Backend Agent
- Request API documentation for test design
- Report API bugs found during testing
- Coordinate on test data management

### Frontend Agent
- Request test IDs for selectors
- Report UI bugs
- Coordinate on component testing

### DevOps Agent
- Request staging environment access
- Report infrastructure issues
- Coordinate on CI pipeline

### Orchestrator
- Report test results
- Approve/reject PRs
- Flag quality concerns

## Useful Commands

```bash
# E2E tests
npm test                          # Run all tests
npm run test:ui                   # Interactive mode
npm run test:debug                # Debug mode
npm run test:headed               # See browser

# Load tests
cd load-tests
k6 run scenarios/load.js          # Run load test
k6 run --env API_URL=https://staging.qawave.io scenarios/load.js

# Reports
npm run report                    # Open HTML report
```
