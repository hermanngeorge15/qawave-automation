# E2E Testing Strategy & Failure Analysis

## Table of Contents
- [Running E2E Tests in Kubernetes](#running-e2e-tests-in-kubernetes)
- [Docker Setup for Playwright](#docker-setup-for-playwright)
- [Test Failure Analysis](#test-failure-analysis)
- [Required Fixes by Agent](#required-fixes-by-agent)

---

## Running E2E Tests in Kubernetes

### Approaches Comparison

| Approach | Pros | Cons | Best For |
|----------|------|------|----------|
| **In-Cluster Job** | No network issues, fast | Requires Docker image, cluster resources | CI/CD pipelines |
| **External Runner** | Easy debugging, local dev | Network latency, firewall issues | Development |
| **Sidecar Container** | Real-time testing | Complex setup | Canary deployments |

### Recommended: Kubernetes Job with Playwright

The best practice is to run E2E tests as a **Kubernetes Job** that:
1. Runs after deployment completes
2. Has network access to services (ClusterIP)
3. Reports results back to CI/CD
4. Cleans up automatically

---

## Docker Setup for Playwright

### Dockerfile for E2E Tests

Create `e2e-tests/Dockerfile`:

```dockerfile
# Use official Playwright image with browsers pre-installed
FROM mcr.microsoft.com/playwright:v1.44.0-jammy

# Set working directory
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy test files
COPY . .

# Set environment variables
ENV CI=true
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

# Default command - can be overridden
CMD ["npx", "playwright", "test", "--config=playwright.staging.config.ts"]
```

### Build and Push Image

```bash
# Build
docker build -t ghcr.io/hermanngeorge15/qawave-e2e-tests:latest ./e2e-tests

# Push to GHCR
docker push ghcr.io/hermanngeorge15/qawave-e2e-tests:latest
```

### Kubernetes Job Manifest

Create `gitops/envs/staging/e2e-tests/e2e-job.yaml`:

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: e2e-tests-${BUILD_ID}
  namespace: staging
  labels:
    app: qawave
    component: e2e-tests
spec:
  ttlSecondsAfterFinished: 3600  # Cleanup after 1 hour
  backoffLimit: 0  # Don't retry failed tests
  template:
    metadata:
      labels:
        app: qawave
        component: e2e-tests
    spec:
      restartPolicy: Never
      imagePullSecrets:
        - name: ghcr-secret
      containers:
        - name: playwright
          image: ghcr.io/hermanngeorge15/qawave-e2e-tests:latest
          imagePullPolicy: Always
          env:
            - name: BASE_URL
              value: "http://frontend.staging.svc.cluster.local"
            - name: API_URL
              value: "http://backend.staging.svc.cluster.local:8080"
            - name: CI
              value: "true"
          resources:
            requests:
              memory: "1Gi"
              cpu: "500m"
            limits:
              memory: "2Gi"
              cpu: "2000m"
          volumeMounts:
            - name: test-results
              mountPath: /app/test-results
            - name: playwright-report
              mountPath: /app/playwright-report
      volumes:
        - name: test-results
          emptyDir: {}
        - name: playwright-report
          emptyDir: {}
```

### CI/CD Integration

Add to `.github/workflows/build-and-deploy.yml`:

```yaml
  e2e-tests:
    needs: [deploy]
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup kubectl
        uses: azure/setup-kubectl@v3

      - name: Configure kubeconfig
        run: |
          mkdir -p ~/.kube
          echo "${{ secrets.KUBECONFIG_STAGING }}" | base64 -d > ~/.kube/config

      - name: Run E2E Tests Job
        run: |
          # Generate unique job name
          JOB_NAME="e2e-tests-${{ github.run_id }}"

          # Create job from template
          cat gitops/envs/staging/e2e-tests/e2e-job.yaml | \
            sed "s/\${BUILD_ID}/${{ github.run_id }}/g" | \
            kubectl apply -f -

          # Wait for job completion (timeout 10 minutes)
          kubectl wait --for=condition=complete job/$JOB_NAME \
            -n staging --timeout=600s || \
          kubectl wait --for=condition=failed job/$JOB_NAME \
            -n staging --timeout=10s

      - name: Get Test Results
        if: always()
        run: |
          POD_NAME=$(kubectl get pods -n staging -l job-name=e2e-tests-${{ github.run_id }} -o jsonpath='{.items[0].metadata.name}')
          kubectl logs $POD_NAME -n staging > e2e-results.log
          cat e2e-results.log

      - name: Upload Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: e2e-test-results
          path: e2e-results.log
```

### Alternative: Run Tests from GitHub Actions Directly

If you prefer running tests from the CI runner (simpler setup):

```yaml
  e2e-tests:
    needs: [deploy]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: e2e-tests/package-lock.json

      - name: Install dependencies
        working-directory: e2e-tests
        run: npm ci

      - name: Install Playwright browsers
        working-directory: e2e-tests
        run: npx playwright install --with-deps chromium

      - name: Run E2E tests
        working-directory: e2e-tests
        env:
          BASE_URL: http://${{ vars.STAGING_WORKER_IP }}:30000
          API_URL: http://${{ vars.STAGING_WORKER_IP }}:30081
        run: npx playwright test --config=playwright.staging.config.ts

      - name: Upload report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: playwright-report
          path: e2e-tests/playwright-report/
```

---

## Test Failure Analysis

### Executive Summary

| Category | Total Tests | Passed | Failed | Pass Rate |
|----------|-------------|--------|--------|-----------|
| API Contract Tests | 26 | 2 | 24 | 7.7% |
| Packages API Tests | 12 | 0 | 12 | 0% |
| UI CRUD Tests | 15 | 1 | 14 | 6.7% |
| **Total** | **53** | **3** | **50** | **5.7%** |

### Test Results by Category

#### 1. API Contract Tests (`src/tests/api/contract.spec.ts`)

##### PASSING (2 tests)
| Test | Status | Notes |
|------|--------|-------|
| `405 Method Not Allowed should have proper format` | PASS | Backend returns proper 405 response |
| `Server errors should not expose internal details` | PASS | Backend error handling is secure |

##### FAILING (24 tests)

**Root Cause: API endpoints not implemented**

| Test Group | Failure Reason | Required Fix |
|------------|----------------|--------------|
| Response Schema Compliance | `/api/qa/packages` returns 404 | Implement GET /api/qa/packages |
| Paginated Response | No pagination response | Implement pagination in list endpoint |
| Request Validation | No POST endpoint | Implement POST /api/qa/packages |
| Error Response Format | 404/400 format incorrect | Implement proper error DTOs |
| Content-Type Handling | No JSON response | Implement proper content-type headers |
| Timestamp Format | No data returned | Implement createdAt/updatedAt fields |
| ID Format | No data returned | Return proper UUID IDs |
| HTTP Status Codes | Wrong status codes | Return 200/201/204 appropriately |

#### 2. Packages API Tests (`src/tests/api/packages.api.spec.ts`)

##### FAILING (12 tests)

**Root Cause: REST API not implemented**

| Test | Expected Behavior | Current Behavior |
|------|-------------------|------------------|
| GET /api/qa/packages | Return paginated list | 404 Not Found |
| GET /api/qa/packages/:id | Return single package | 404 Not Found |
| POST /api/qa/packages | Create package, return 201 | 404 Not Found |
| PUT /api/qa/packages/:id | Update package, return 200 | 404 Not Found |
| DELETE /api/qa/packages/:id | Delete package, return 204 | 404 Not Found |
| POST /api/qa/packages/:id/runs | Trigger test run, return 202 | 404 Not Found |

#### 3. UI CRUD Tests (`src/tests/packages/crud.spec.ts`)

##### PASSING (1 test)
| Test | Status | Notes |
|------|--------|-------|
| `should verify pagination controls` | PASS | Pagination UI exists |

##### FAILING (14 tests)

**Root Cause: UI components not implemented or different selectors**

| Test | Failure Reason | Expected Element |
|------|----------------|------------------|
| Create package with valid data | Timeout waiting for form | Create package form/modal |
| Show validation error for empty URL | Form not found | Validation error display |
| Show validation error for invalid URL | Form not found | URL validation |
| Cancel package creation | Form not found | Cancel button |
| Display packages list | No package cards | Package list items |
| Search packages by name | Search not working | Search functionality |
| Show empty state | Empty state not found | Empty state component |
| Display package details | Details page timeout | Package detail view |
| Navigate between tabs | Tabs not found | Tab navigation |

---

## Required Fixes by Agent

### Backend Agent (@agent:backend)

#### Priority 1: Implement Package REST API

**File to create/modify:** `backend/src/main/kotlin/com/qawave/api/controller/PackageController.kt`

```kotlin
@RestController
@RequestMapping("/api/qa/packages")
class PackageController(
    private val packageService: PackageService
) {

    @GetMapping
    suspend fun listPackages(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PagedResponse<PackageDto>> {
        // Return paginated list
    }

    @GetMapping("/{id}")
    suspend fun getPackage(@PathVariable id: UUID): ResponseEntity<PackageDto> {
        // Return single package or 404
    }

    @PostMapping
    suspend fun createPackage(
        @Valid @RequestBody request: CreatePackageRequest
    ): ResponseEntity<PackageDto> {
        // Return 201 with created package
    }

    @PutMapping("/{id}")
    suspend fun updatePackage(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdatePackageRequest
    ): ResponseEntity<PackageDto> {
        // Return 200 with updated package
    }

    @DeleteMapping("/{id}")
    suspend fun deletePackage(@PathVariable id: UUID): ResponseEntity<Unit> {
        // Return 204 No Content
    }

    @PostMapping("/{id}/runs")
    suspend fun triggerRun(@PathVariable id: UUID): ResponseEntity<RunDto> {
        // Return 202 Accepted with run info
    }
}
```

#### Priority 2: Implement DTOs

**Required DTOs:**

```kotlin
// PackageDto.kt
data class PackageDto(
    val id: UUID,
    val name: String,
    val specUrl: String,
    val description: String?,
    val status: PackageStatus,
    val createdAt: Instant,  // ISO 8601 format
    val updatedAt: Instant
)

// PagedResponse.kt
data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
)

// CreatePackageRequest.kt
data class CreatePackageRequest(
    @field:NotBlank val name: String,
    @field:NotBlank @field:URL val specUrl: String,
    val description: String?
)

// ErrorResponse.kt
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: Instant,
    val path: String,
    val validationErrors: List<ValidationError>? = null
)
```

#### Priority 3: Implement Validation

- Return 400 Bad Request for invalid input
- Include validation error details in response
- Validate URL format for specUrl field
- Reject names longer than 255 characters

---

### Frontend Agent (@agent:frontend)

#### Priority 1: Implement Package List Page

**File:** `frontend/src/pages/PackagesPage.tsx`

**Required elements (data-testid attributes for tests):**

```tsx
// Package list page
<div data-testid="packages-page">
  {/* Search input */}
  <input
    data-testid="search-input"
    placeholder="Search packages..."
  />

  {/* Create button */}
  <button data-testid="create-package-button">
    Create Package
  </button>

  {/* Package cards */}
  {packages.map(pkg => (
    <div key={pkg.id} data-testid="package-card">
      <span data-testid="package-name">{pkg.name}</span>
      <span data-testid="package-status">{pkg.status}</span>
    </div>
  ))}

  {/* Empty state */}
  {packages.length === 0 && (
    <div data-testid="empty-state">No packages found</div>
  )}

  {/* Pagination */}
  <div data-testid="pagination">
    <button data-testid="prev-page">Previous</button>
    <span data-testid="page-info">Page {page} of {totalPages}</span>
    <button data-testid="next-page">Next</button>
  </div>
</div>
```

#### Priority 2: Implement Create Package Modal/Form

**File:** `frontend/src/components/CreatePackageModal.tsx`

**Required elements:**

```tsx
<dialog data-testid="create-package-modal">
  <form data-testid="create-package-form">
    {/* Name input */}
    <input
      data-testid="package-name-input"
      name="name"
      placeholder="Package name"
    />

    {/* Spec URL input */}
    <input
      data-testid="spec-url-input"
      name="specUrl"
      placeholder="OpenAPI spec URL"
    />

    {/* Description input */}
    <textarea
      data-testid="description-input"
      name="description"
      placeholder="Description (optional)"
    />

    {/* Validation errors */}
    <div data-testid="validation-error" role="alert">
      {error}
    </div>

    {/* Buttons */}
    <button type="submit" data-testid="submit-button">
      Create
    </button>
    <button type="button" data-testid="cancel-button">
      Cancel
    </button>
  </form>
</dialog>
```

#### Priority 3: Implement Package Detail Page

**File:** `frontend/src/pages/PackageDetailPage.tsx`

**Required elements:**

```tsx
<div data-testid="package-detail-page">
  {/* Header */}
  <h1 data-testid="package-title">{package.name}</h1>
  <span data-testid="package-status">{package.status}</span>

  {/* Tabs */}
  <nav data-testid="tabs">
    <button data-testid="tab-overview">Overview</button>
    <button data-testid="tab-scenarios">Scenarios</button>
    <button data-testid="tab-runs">Test Runs</button>
    <button data-testid="tab-settings">Settings</button>
  </nav>

  {/* Tab content */}
  <div data-testid="tab-content">
    {/* Dynamic content based on selected tab */}
  </div>

  {/* Actions */}
  <button data-testid="run-tests-button">Run Tests</button>
  <button data-testid="edit-button">Edit</button>
  <button data-testid="delete-button">Delete</button>
</div>
```

---

### QA Agent (@agent:qa)

#### Update Test Selectors

If the frontend uses different selectors, update the Page Object files:

**File:** `e2e-tests/src/pages/PackagesPage.ts`

```typescript
export class PackagesPage {
  readonly page: Page;

  // Update these selectors to match actual frontend implementation
  readonly searchInput = this.page.getByTestId('search-input');
  readonly createButton = this.page.getByTestId('create-package-button');
  readonly packageCards = this.page.getByTestId('package-card');
  readonly emptyState = this.page.getByTestId('empty-state');
  readonly pagination = this.page.getByTestId('pagination');

  // Alternative selectors if data-testid not used
  // readonly searchInput = this.page.getByPlaceholder('Search packages');
  // readonly createButton = this.page.getByRole('button', { name: 'Create' });
}
```

---

## API Contract Specification

For reference, here's the expected API contract that tests are validating:

### Endpoints

| Method | Path | Request Body | Response | Status |
|--------|------|--------------|----------|--------|
| GET | /api/qa/packages | - | PagedResponse<Package> | 200 |
| GET | /api/qa/packages/:id | - | Package | 200/404 |
| POST | /api/qa/packages | CreatePackageRequest | Package | 201/400 |
| PUT | /api/qa/packages/:id | UpdatePackageRequest | Package | 200/404 |
| DELETE | /api/qa/packages/:id | - | - | 204/404 |
| POST | /api/qa/packages/:id/runs | - | Run | 202/404 |

### Response Headers

All JSON responses must include:
```
Content-Type: application/json
```

### Error Response Format

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "2024-01-27T12:00:00Z",
  "path": "/api/qa/packages",
  "validationErrors": [
    {
      "field": "specUrl",
      "message": "must be a valid URL"
    }
  ]
}
```

---

## Quick Reference Commands

### Run E2E Tests Locally Against Staging

```bash
cd e2e-tests
npm install
BASE_URL=http://46.224.232.46:30000 API_URL=http://localhost:8080 \
  npx playwright test --config=playwright.staging.config.ts
```

### Run Specific Test File

```bash
npx playwright test src/tests/api/packages.api.spec.ts
```

### Run Tests with UI Mode (Debug)

```bash
npx playwright test --ui
```

### View HTML Report

```bash
npx playwright show-report
```

---

## Files to Create/Modify Summary

| Agent | File | Action |
|-------|------|--------|
| Backend | `controller/PackageController.kt` | CREATE |
| Backend | `dto/PackageDto.kt` | CREATE |
| Backend | `dto/PagedResponse.kt` | CREATE |
| Backend | `dto/ErrorResponse.kt` | CREATE |
| Backend | `service/PackageService.kt` | CREATE |
| Frontend | `pages/PackagesPage.tsx` | MODIFY |
| Frontend | `components/CreatePackageModal.tsx` | CREATE |
| Frontend | `pages/PackageDetailPage.tsx` | CREATE |
| DevOps | `e2e-tests/Dockerfile` | CREATE |
| DevOps | `gitops/envs/staging/e2e-tests/e2e-job.yaml` | CREATE |
| QA | `e2e-tests/src/pages/*.ts` | MODIFY (if needed) |
