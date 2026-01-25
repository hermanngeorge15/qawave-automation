import { test, expect } from '@playwright/test';
import { PackagesPage } from '../../pages/PackagesPage';
import { PackageDetailPage } from '../../pages/PackageDetailPage';
import { testData } from '../../fixtures/testData';
import { cleanupPackage } from '../../fixtures/cleanup';

/**
 * Smoke tests for QAWave critical paths
 * These tests verify the most important user journeys work correctly
 *
 * @tags @smoke
 */
test.describe('Smoke Tests @smoke', () => {
  test.describe.configure({ mode: 'serial' });

  let createdPackageId: string | null = null;

  test.afterAll(async () => {
    // Cleanup any created test data
    if (createdPackageId) {
      await cleanupPackage(createdPackageId);
    }
  });

  test('application loads successfully', async ({ page }) => {
    // Navigate to home page
    await page.goto('/');

    // Verify the page loads without errors
    await expect(page).toHaveTitle(/QAWave/);

    // Verify main navigation is visible
    const nav = page.getByRole('navigation');
    await expect(nav).toBeVisible();
  });

  test('can navigate to packages list', async ({ page }) => {
    const packagesPage = new PackagesPage(page);

    // Navigate to packages page
    await packagesPage.navigate();

    // Verify URL
    await expect(page).toHaveURL(/\/packages/);

    // Verify create button is visible
    await expect(packagesPage.createButton).toBeVisible();

    // Verify search input is visible
    await expect(packagesPage.searchInput).toBeVisible();
  });

  test('can create a new QA package', async ({ page }) => {
    const packagesPage = new PackagesPage(page);
    const packageData = testData.createPackage();

    // Navigate to packages
    await packagesPage.navigate();
    await packagesPage.waitForPackagesLoaded();

    // Click create button
    await packagesPage.clickCreatePackage();

    // Fill in package details in modal
    const modal = page.getByRole('dialog');
    await expect(modal).toBeVisible();

    // Fill form fields
    await modal.getByLabel('Name').fill(packageData.name);
    await modal.getByLabel('Spec URL').fill(packageData.specUrl);

    if (packageData.baseUrl) {
      await modal.getByLabel('Base URL').fill(packageData.baseUrl);
    }

    if (packageData.description) {
      await modal.getByLabel('Description').fill(packageData.description);
    }

    // Submit form
    await modal.getByRole('button', { name: 'Create' }).click();

    // Wait for navigation to package detail page
    await expect(page).toHaveURL(/\/packages\/[\w-]+/);

    // Store package ID for cleanup
    const url = page.url();
    const match = url.match(/\/packages\/([\w-]+)/);
    createdPackageId = match ? match[1] : null;

    // Verify package name is displayed
    await expect(page.getByText(packageData.name)).toBeVisible();
  });

  test('can view package details', async ({ page }) => {
    test.skip(!createdPackageId, 'No package created in previous test');

    const detailPage = new PackageDetailPage(page);

    // Navigate to package detail
    await detailPage.navigateToPackage(createdPackageId!);
    await detailPage.waitForLoad();

    // Verify package details are displayed
    await expect(detailPage.packageName).toBeVisible();
    await expect(detailPage.specUrl).toBeVisible();

    // Verify action buttons are available
    await expect(detailPage.editButton).toBeVisible();
    await expect(detailPage.runTestsButton).toBeVisible();
  });

  test('can trigger a test run', async ({ page }) => {
    test.skip(!createdPackageId, 'No package created in previous test');

    const detailPage = new PackageDetailPage(page);

    // Navigate to package detail
    await detailPage.navigateToPackage(createdPackageId!);
    await detailPage.waitForLoad();

    // Click run tests button
    await detailPage.runTests();

    // Wait for run to start - should navigate to run detail or show status
    await page.waitForResponse(
      (response) =>
        response.url().includes('/runs') && response.status() < 400,
      { timeout: 30000 }
    );

    // Verify run was initiated (either navigate to run page or see status update)
    const runIndicator = page
      .getByTestId('run-status')
      .or(page.getByText(/running|queued/i));
    await expect(runIndicator).toBeVisible({ timeout: 10000 });
  });

  test('can view run results', async ({ page }) => {
    test.skip(!createdPackageId, 'No package created in previous test');

    const detailPage = new PackageDetailPage(page);

    // Navigate to package detail
    await detailPage.navigateToPackage(createdPackageId!);
    await detailPage.waitForLoad();

    // Go to runs tab
    await detailPage.goToRunsTab();

    // Verify runs list is visible
    const runsList = page.getByTestId('runs-list').or(page.getByRole('table'));
    await expect(runsList).toBeVisible({ timeout: 10000 });

    // Should have at least one run from previous test
    const runRows = page.getByTestId('run-row').or(page.getByRole('row'));
    await expect(runRows.first()).toBeVisible();
  });
});
