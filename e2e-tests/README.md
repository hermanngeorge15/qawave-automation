# QAWave E2E Tests

End-to-end tests for the QAWave platform using Playwright.

## Prerequisites

- Node.js 18+
- npm 9+

## Installation

```bash
cd e2e-tests
npm install
npx playwright install
```

## Running Tests

### All tests

```bash
npm test
```

### With UI mode (interactive)

```bash
npm run test:ui
```

### Headed mode (see browser)

```bash
npm run test:headed
```

### Debug mode

```bash
npm run test:debug
```

### Smoke tests only

```bash
npm run test:smoke
```

### API tests only

```bash
npm run test:api
```

### Specific browser

```bash
npm run test:chromium
npm run test:firefox
npm run test:webkit
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `BASE_URL` | `http://localhost:5173` | Frontend URL |
| `API_URL` | `http://localhost:8080` | Backend API URL |
| `CI` | - | Set to `true` in CI environment |

### Running against different environments

```bash
# Local
npm test

# Staging
BASE_URL=https://staging.qawave.io API_URL=https://api.staging.qawave.io npm test

# Production (smoke tests only)
BASE_URL=https://qawave.io API_URL=https://api.qawave.io npm run test:smoke
```

## Project Structure

```
e2e-tests/
├── src/
│   ├── fixtures/         # Test data and setup helpers
│   ├── pages/            # Page Object Models
│   ├── api/              # API client helpers
│   ├── tests/            # Test specifications
│   │   ├── smoke/        # Critical path tests
│   │   ├── packages/     # Package feature tests
│   │   ├── scenarios/    # Scenario feature tests
│   │   └── api/          # Pure API tests
│   └── utils/            # Utilities
├── load-tests/           # k6 performance tests
├── playwright.config.ts
└── package.json
```

## Writing Tests

### Page Object Model

All page interactions should go through Page Object Models:

```typescript
import { PackagesPage } from '../../pages/PackagesPage';

test('example test', async ({ page }) => {
  const packagesPage = new PackagesPage(page);
  await packagesPage.navigate();
  await packagesPage.clickCreatePackage();
});
```

### Test Data

Use test data factories for consistent, realistic data:

```typescript
import { testData } from '../../fixtures/testData';

const packageData = testData.createPackage();
```

### Cleanup

Always clean up test data:

```typescript
import { cleanupPackage } from '../../fixtures/cleanup';

test.afterEach(async () => {
  if (createdPackageId) {
    await cleanupPackage(createdPackageId);
  }
});
```

## CI Integration

Tests run automatically in GitHub Actions when:

- A PR is created
- Code is pushed to main
- Staging deployment completes

## Reports

View the HTML report:

```bash
npm run report
```

Reports are also available in CI artifacts.

## Troubleshooting

### Tests fail on CI but pass locally

- Check environment variables
- Verify network access to test endpoints
- Check for timing issues (increase timeouts if needed)

### Element not found errors

- Verify test IDs match component implementation
- Check if element is in viewport
- Ensure page has fully loaded

### Flaky tests

- Use proper waits instead of arbitrary timeouts
- Make tests independent and isolated
- Clean up test data between runs
