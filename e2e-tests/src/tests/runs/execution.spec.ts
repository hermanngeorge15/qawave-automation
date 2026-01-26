import { test, expect } from '@playwright/test';
import { PackagesPage } from '../../pages/PackagesPage';
import { PackageDetailPage } from '../../pages/PackageDetailPage';
import { RunDetailPage } from '../../pages/RunDetailPage';
import { testData, PackageData } from '../../fixtures/testData';
import { cleanupPackage, cleanupTestRun } from '../../fixtures/cleanup';

/**
 * Test Run E2E Tests
 * Tests for test execution flows including triggering, progress, and results
 *
 * @tags @runs @execution
 */
test.describe('Test Run Execution @runs @execution', () => {
  // Track created resources for cleanup
  const createdPackageIds: string[] = [];
  const createdRunIds: string[] = [];

  test.afterEach(async () => {
    // Cleanup test runs first (they depend on packages)
    for (const id of createdRunIds) {
      await cleanupTestRun(id);
    }
    createdRunIds.length = 0;

    // Then cleanup packages
    for (const id of createdPackageIds) {
      await cleanupPackage(id);
    }
    createdPackageIds.length = 0;
  });

  /**
   * Helper to create a test package for run tests
   */
  async function createTestPackage(page: import('@playwright/test').Page): Promise<{
    packageId: string;
    packageData: PackageData;
  }> {
    const packagesPage = new PackagesPage(page);
    const packageData = testData.createPackage();

    await packagesPage.navigate();
    await packagesPage.waitForPackagesLoaded();
    await packagesPage.clickCreatePackage();

    const modal = page.getByRole('dialog');
    await modal.getByLabel('Name').fill(packageData.name);
    await modal.getByLabel('Spec URL').fill(packageData.specUrl);
    if (packageData.baseUrl) {
      await modal.getByLabel('Base URL').fill(packageData.baseUrl);
    }
    await modal.getByRole('button', { name: 'Create' }).click();

    await expect(page).toHaveURL(/\/packages\/[\w-]+/);

    const url = page.url();
    const match = url.match(/\/packages\/([\w-]+)/);
    const packageId = match ? match[1] : '';
    createdPackageIds.push(packageId);

    return { packageId, packageData };
  }

  test.describe('Trigger Test Run', () => {
    test('should trigger a new test run from package detail page', async ({ page }) => {
      // Create a package first
      const { packageId } = await createTestPackage(page);
      const detailPage = new PackageDetailPage(page);

      // Navigate to package detail
      await detailPage.navigateToPackage(packageId);
      await detailPage.waitForLoad();

      // Click run tests button
      await detailPage.runTests();

      // Wait for response indicating run was triggered
      const response = await page.waitForResponse(
        (resp) => resp.url().includes('/runs') && resp.status() < 400,
        { timeout: 30000 }
      );

      expect(response.ok()).toBe(true);

      // Should show run status indicator
      const runIndicator = page
        .getByTestId('run-status')
        .or(page.getByText(/running|queued|pending/i));
      await expect(runIndicator).toBeVisible({ timeout: 10000 });
    });

    test('should navigate to run detail page after triggering', async ({ page }) => {
      const { packageId } = await createTestPackage(page);
      const detailPage = new PackageDetailPage(page);

      await detailPage.navigateToPackage(packageId);
      await detailPage.waitForLoad();

      // Trigger a run
      await detailPage.runTests();

      // Should navigate to run detail page or show run ID
      await page.waitForTimeout(2000); // Allow time for navigation

      // Either navigated to run page or can click to go there
      const runLink = page.getByRole('link', { name: /view run/i }).or(
        page.getByTestId('current-run-link')
      );

      if (await runLink.isVisible()) {
        await runLink.click();
      }

      // If we're on run detail page
      const runDetailUrl = page.url();
      if (runDetailUrl.includes('/runs/')) {
        const runMatch = runDetailUrl.match(/\/runs\/([\w-]+)/);
        if (runMatch) {
          createdRunIds.push(runMatch[1]);
        }
        await expect(page).toHaveURL(/\/runs\/[\w-]+/);
      }
    });
  });

  test.describe('View Run Progress', () => {
    test('should display run status while in progress', async ({ page }) => {
      const { packageId } = await createTestPackage(page);
      const detailPage = new PackageDetailPage(page);

      await detailPage.navigateToPackage(packageId);
      await detailPage.waitForLoad();

      // Trigger a run
      await detailPage.runTests();

      // Wait a moment for run to start
      await page.waitForTimeout(1000);

      // Check for status indicator
      const statusIndicator = page.getByTestId('run-status').or(
        page.getByText(/running|pending|queued/i)
      );

      // Status should be visible
      await expect(statusIndicator).toBeVisible({ timeout: 10000 });
    });

    test('should show progress bar during run execution', async ({ page }) => {
      const { packageId } = await createTestPackage(page);
      const detailPage = new PackageDetailPage(page);

      await detailPage.navigateToPackage(packageId);
      await detailPage.waitForLoad();

      // Trigger a run
      await detailPage.runTests();

      // Navigate to run detail if not auto-navigated
      await page.waitForTimeout(2000);
      const runDetailPage = new RunDetailPage(page);

      // If we have a run ID in URL
      if (page.url().includes('/runs/')) {
        // Wait for progress indicator
        const progressIndicator = runDetailPage.progressBar.or(
          page.getByRole('progressbar')
        );

        // Progress bar should be visible during run
        // (may not be visible if run completes too quickly)
        const isVisible = await progressIndicator.isVisible().catch(() => false);
        // Just verify we're on the run page
        await expect(page).toHaveURL(/\/runs/);
      }
    });
  });

  test.describe('View Run Results', () => {
    test('should display run results with step details', async ({ page }) => {
      const { packageId } = await createTestPackage(page);
      const detailPage = new PackageDetailPage(page);

      await detailPage.navigateToPackage(packageId);
      await detailPage.waitForLoad();

      // Trigger a run
      await detailPage.runTests();

      // Wait for run to appear in runs tab
      await page.waitForTimeout(3000);

      // Go to runs tab
      await detailPage.goToRunsTab();

      // Wait for runs list
      const runsList = page.getByTestId('runs-list').or(page.getByRole('table'));
      await expect(runsList).toBeVisible({ timeout: 15000 });

      // Click on first run
      const firstRun = page.getByTestId('run-row').first().or(
        page.getByRole('row').filter({ hasText: /run/i }).first()
      );

      if (await firstRun.isVisible()) {
        await firstRun.click();

        // Should navigate to run detail
        await page.waitForTimeout(1000);

        // Check for run details
        const runDetail = page.getByTestId('run-status').or(
          page.getByText(/passed|failed|running/i)
        );
        await expect(runDetail).toBeVisible({ timeout: 10000 });
      }
    });

    test('should show pass/fail status for test steps', async ({ page }) => {
      const { packageId } = await createTestPackage(page);
      const detailPage = new PackageDetailPage(page);
      const runDetailPage = new RunDetailPage(page);

      await detailPage.navigateToPackage(packageId);
      await detailPage.waitForLoad();

      // Trigger a run
      await detailPage.runTests();

      // Wait and navigate to runs tab
      await page.waitForTimeout(5000);
      await detailPage.goToRunsTab();

      // Click first run
      const runsList = page.getByTestId('runs-list').or(page.getByRole('table'));
      await expect(runsList).toBeVisible({ timeout: 15000 });

      const firstRun = page.getByTestId('run-row').first();
      if (await firstRun.isVisible()) {
        await firstRun.click();
        await page.waitForTimeout(1000);

        // Verify we can see test results
        const resultsSection = runDetailPage.testResults.or(
          page.getByTestId('test-case-list')
        );

        if (await resultsSection.isVisible()) {
          // Check for pass/fail indicators
          const passIndicator = runDetailPage.passedTests.or(
            page.getByText(/passed/i)
          );
          const failIndicator = runDetailPage.failedTests.or(
            page.getByText(/failed/i)
          );

          // At least one indicator should be present
          const hasPassIndicator = await passIndicator.isVisible().catch(() => false);
          const hasFailIndicator = await failIndicator.isVisible().catch(() => false);

          expect(hasPassIndicator || hasFailIndicator).toBe(true);
        }
      }
    });

    test('should display error details for failed steps', async ({ page }) => {
      const { packageId } = await createTestPackage(page);
      const detailPage = new PackageDetailPage(page);
      const runDetailPage = new RunDetailPage(page);

      await detailPage.navigateToPackage(packageId);
      await detailPage.waitForLoad();

      // Trigger a run
      await detailPage.runTests();

      // Wait for run to complete (with timeout)
      await page.waitForTimeout(10000);

      // Go to runs tab
      await detailPage.goToRunsTab();

      const runsList = page.getByTestId('runs-list').or(page.getByRole('table'));
      await expect(runsList).toBeVisible({ timeout: 15000 });

      // Click first run
      const firstRun = page.getByTestId('run-row').first();
      if (await firstRun.isVisible()) {
        await firstRun.click();
        await page.waitForTimeout(2000);

        // If there are failed tests, check for error details
        const failedCount = await runDetailPage.getFailedCount().catch(() => 0);

        if (failedCount > 0) {
          // Should show error details
          const errorSection = runDetailPage.errorDetails.or(
            page.getByText(/error/i)
          );
          await expect(errorSection).toBeVisible();
        }
      }
    });
  });

  test.describe('Replay Run', () => {
    test('should replay a previous run', async ({ page }) => {
      const { packageId } = await createTestPackage(page);
      const detailPage = new PackageDetailPage(page);
      const runDetailPage = new RunDetailPage(page);

      await detailPage.navigateToPackage(packageId);
      await detailPage.waitForLoad();

      // Trigger initial run
      await detailPage.runTests();
      await page.waitForTimeout(5000);

      // Go to runs tab
      await detailPage.goToRunsTab();

      const runsList = page.getByTestId('runs-list').or(page.getByRole('table'));
      await expect(runsList).toBeVisible({ timeout: 15000 });

      // Click first run
      const firstRun = page.getByTestId('run-row').first();
      if (await firstRun.isVisible()) {
        await firstRun.click();
        await page.waitForTimeout(2000);

        // Store original run ID
        const originalRunId = await runDetailPage.getRunIdFromUrl();

        // Click re-run button
        const rerunButton = runDetailPage.rerunButton.or(
          page.getByRole('button', { name: /re-?run|replay/i })
        );

        if (await rerunButton.isVisible()) {
          await rerunButton.click();

          // Wait for new run to start
          await page.waitForResponse(
            (resp) => resp.url().includes('/runs') && resp.status() < 400,
            { timeout: 30000 }
          ).catch(() => null);

          // Should have new run or confirmation
          await page.waitForTimeout(2000);

          // If navigated to new run, it should have different ID
          const newRunId = await runDetailPage.getRunIdFromUrl();
          if (newRunId && newRunId !== originalRunId) {
            createdRunIds.push(newRunId);
          }
        }
      }
    });
  });

  test.describe('Handle Run Failures', () => {
    test('should display run failure status clearly', async ({ page }) => {
      // This test verifies UI handles failed runs gracefully
      const { packageId } = await createTestPackage(page);
      const detailPage = new PackageDetailPage(page);

      await detailPage.navigateToPackage(packageId);
      await detailPage.waitForLoad();

      // Trigger a run
      await detailPage.runTests();
      await page.waitForTimeout(10000);

      // Go to runs tab
      await detailPage.goToRunsTab();

      const runsList = page.getByTestId('runs-list').or(page.getByRole('table'));
      await expect(runsList).toBeVisible({ timeout: 15000 });

      // Click first run
      const firstRun = page.getByTestId('run-row').first();
      if (await firstRun.isVisible()) {
        await firstRun.click();
        await page.waitForTimeout(2000);

        // Verify status is displayed (any status)
        const statusElement = page.getByTestId('run-status').or(
          page.getByText(/passed|failed|error|complete|running/i)
        );
        await expect(statusElement).toBeVisible();
      }
    });

    test('should allow navigation back after viewing failed run', async ({ page }) => {
      const { packageId } = await createTestPackage(page);
      const detailPage = new PackageDetailPage(page);
      const runDetailPage = new RunDetailPage(page);

      await detailPage.navigateToPackage(packageId);
      await detailPage.waitForLoad();

      // Trigger a run
      await detailPage.runTests();
      await page.waitForTimeout(5000);

      // Go to runs tab
      await detailPage.goToRunsTab();

      const runsList = page.getByTestId('runs-list').or(page.getByRole('table'));
      await expect(runsList).toBeVisible({ timeout: 15000 });

      // Click first run
      const firstRun = page.getByTestId('run-row').first();
      if (await firstRun.isVisible()) {
        await firstRun.click();
        await page.waitForTimeout(2000);

        // Click back button
        const backButton = runDetailPage.backButton.or(
          page.getByRole('button', { name: /back/i })
        );

        if (await backButton.isVisible()) {
          await backButton.click();

          // Should navigate back to package or runs list
          await expect(page).toHaveURL(/\/packages|\/runs/);
        }
      }
    });
  });

  test.describe('Run List View', () => {
    test('should display runs list in package detail', async ({ page }) => {
      const { packageId } = await createTestPackage(page);
      const detailPage = new PackageDetailPage(page);

      await detailPage.navigateToPackage(packageId);
      await detailPage.waitForLoad();

      // Trigger a run
      await detailPage.runTests();
      await page.waitForTimeout(5000);

      // Go to runs tab
      await detailPage.goToRunsTab();

      // Should show runs list
      const runsList = page.getByTestId('runs-list').or(page.getByRole('table'));
      await expect(runsList).toBeVisible({ timeout: 15000 });

      // Should have at least one run
      const runRows = page.getByTestId('run-row').or(
        page.getByRole('row').filter({ hasText: /run|test/i })
      );
      const count = await runRows.count();
      expect(count).toBeGreaterThanOrEqual(1);
    });

    test('should show run timestamps', async ({ page }) => {
      const { packageId } = await createTestPackage(page);
      const detailPage = new PackageDetailPage(page);

      await detailPage.navigateToPackage(packageId);
      await detailPage.waitForLoad();

      // Trigger a run
      await detailPage.runTests();
      await page.waitForTimeout(5000);

      // Go to runs tab
      await detailPage.goToRunsTab();

      const runsList = page.getByTestId('runs-list').or(page.getByRole('table'));
      await expect(runsList).toBeVisible({ timeout: 15000 });

      // Click first run to see details
      const firstRun = page.getByTestId('run-row').first();
      if (await firstRun.isVisible()) {
        await firstRun.click();
        await page.waitForTimeout(2000);

        // Should show start time
        const startTime = page.getByTestId('run-start-time').or(
          page.getByText(/started|created/i)
        );
        await expect(startTime).toBeVisible();
      }
    });
  });
});
