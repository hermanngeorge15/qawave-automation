import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright Configuration for Staging Environment
 *
 * Optimized for remote testing against staging environment
 * - Longer timeouts (network latency)
 * - Fewer workers (don't overload staging)
 * - Chromium only (faster feedback)
 * - No local webserver
 *
 * Usage:
 *   BASE_URL=http://<LB_IP> API_URL=http://<LB_IP>:8080 npx playwright test --config=playwright.staging.config.ts
 *
 * Environment variables (REQUIRED - no DNS):
 *   BASE_URL - Frontend URL (e.g., http://168.119.x.x or http://168.119.x.x.nip.io)
 *   API_URL  - Backend API URL (e.g., http://168.119.x.x:8080)
 */
export default defineConfig({
  testDir: './src/tests',

  /* Run tests in files in parallel */
  fullyParallel: true,

  /* Fail the build on CI if you accidentally left test.only in the source code */
  forbidOnly: !!process.env.CI,

  /* More retries for staging (network latency, cold starts) */
  retries: 3,

  /* Fewer workers to not overload staging environment */
  workers: 2,

  /* Reporter configuration */
  reporter: [
    ['html', { open: 'never', outputFolder: 'playwright-report' }],
    ['junit', { outputFile: 'results/staging-junit.xml' }],
    ['list'],
  ],

  /* Shared settings for all projects */
  use: {
    /* Base URL for staging frontend - MUST be set via environment variable */
    baseURL: process.env.BASE_URL,

    /* Extra HTTP headers */
    extraHTTPHeaders: {
      'Accept': 'application/json',
    },

    /* Always collect trace for staging (helps debug remote issues) */
    trace: 'retain-on-failure',

    /* Screenshot on failure */
    screenshot: 'only-on-failure',

    /* Record video on failure (useful for remote debugging) */
    video: 'retain-on-failure',

    /* Longer timeouts for remote staging environment */
    actionTimeout: 30000,      // 30s (vs 15s local)
    navigationTimeout: 60000,  // 60s (vs 30s local)
  },

  /* Global timeout for each test - longer for staging */
  timeout: 90000,  // 90s (vs 60s local)

  /* Expect timeout - longer for staging */
  expect: {
    timeout: 15000,  // 15s (vs 10s local)
  },

  /* Only Chromium for staging (faster feedback loop) */
  projects: [
    {
      name: 'staging-chromium',
      use: {
        ...devices['Desktop Chrome'],
        // Viewport for consistent screenshots
        viewport: { width: 1280, height: 720 },
      },
    },
    /* API testing project */
    {
      name: 'staging-api',
      testDir: './src/tests/api',
      use: {
        /* API URL - MUST be set via environment variable */
        baseURL: process.env.API_URL,
      },
    },
  ],

  /* No local webserver - we test against deployed staging */
  webServer: undefined,

  /* Output folder for test artifacts */
  outputDir: 'test-results/',

  /* Global setup/teardown if needed */
  // globalSetup: './src/setup/staging.setup.ts',
  // globalTeardown: './src/setup/staging.teardown.ts',
});
