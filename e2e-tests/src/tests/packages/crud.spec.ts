import { test, expect } from '@playwright/test';
import { PackagesPage } from '../../pages/PackagesPage';
import { PackageDetailPage } from '../../pages/PackageDetailPage';
import { testData, PackageData } from '../../fixtures/testData';
import { cleanupPackage } from '../../fixtures/cleanup';

/**
 * Package CRUD E2E Tests
 * Tests for Create, Read, Update, Delete operations on QA Packages
 *
 * @tags @packages @crud
 */
test.describe('Package CRUD Operations @packages @crud', () => {
  // Track created packages for cleanup
  const createdPackageIds: string[] = [];

  test.afterEach(async () => {
    // Cleanup all created packages after each test
    for (const id of createdPackageIds) {
      await cleanupPackage(id);
    }
    createdPackageIds.length = 0;
  });

  test.describe('Create Package', () => {
    test('should create a new package with valid data', async ({ page }) => {
      const packagesPage = new PackagesPage(page);
      const packageData = testData.createPackage();

      // Navigate to packages list
      await packagesPage.navigate();
      await packagesPage.waitForPackagesLoaded();

      // Click create button
      await packagesPage.clickCreatePackage();

      // Verify modal is displayed
      const modal = page.getByRole('dialog');
      await expect(modal).toBeVisible();

      // Fill in package details
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

      // Extract package ID from URL
      const url = page.url();
      const match = url.match(/\/packages\/([\w-]+)/);
      if (match) {
        createdPackageIds.push(match[1]);
      }

      // Verify package name is displayed
      await expect(page.getByText(packageData.name)).toBeVisible();

      // Verify spec URL is displayed
      await expect(page.getByText(packageData.specUrl)).toBeVisible();
    });

    test('should show validation error for empty spec URL', async ({ page }) => {
      const packagesPage = new PackagesPage(page);
      const packageData = testData.createPackage();

      await packagesPage.navigate();
      await packagesPage.waitForPackagesLoaded();
      await packagesPage.clickCreatePackage();

      const modal = page.getByRole('dialog');
      await expect(modal).toBeVisible();

      // Fill only name, leave spec URL empty
      await modal.getByLabel('Name').fill(packageData.name);
      // Ensure spec URL is empty
      await modal.getByLabel('Spec URL').fill('');

      // Submit form
      await modal.getByRole('button', { name: 'Create' }).click();

      // Expect validation error
      const specUrlError = modal.getByText(/spec url is required/i).or(
        modal.getByRole('alert').filter({ hasText: /spec url/i })
      );
      await expect(specUrlError).toBeVisible();

      // Modal should still be open
      await expect(modal).toBeVisible();
    });

    test('should show validation error for invalid URL format', async ({ page }) => {
      const packagesPage = new PackagesPage(page);
      const packageData = testData.createPackage();

      await packagesPage.navigate();
      await packagesPage.waitForPackagesLoaded();
      await packagesPage.clickCreatePackage();

      const modal = page.getByRole('dialog');
      await expect(modal).toBeVisible();

      // Fill with invalid URL
      await modal.getByLabel('Name').fill(packageData.name);
      await modal.getByLabel('Spec URL').fill('not-a-valid-url');

      // Submit form
      await modal.getByRole('button', { name: 'Create' }).click();

      // Expect validation error for invalid URL
      const urlError = modal.getByText(/invalid url/i).or(
        modal.getByRole('alert').filter({ hasText: /url/i })
      );
      await expect(urlError).toBeVisible();
    });

    test('should cancel package creation', async ({ page }) => {
      const packagesPage = new PackagesPage(page);

      await packagesPage.navigate();
      await packagesPage.waitForPackagesLoaded();
      await packagesPage.clickCreatePackage();

      const modal = page.getByRole('dialog');
      await expect(modal).toBeVisible();

      // Click cancel button or close button
      const cancelButton = modal.getByRole('button', { name: 'Cancel' }).or(
        modal.getByRole('button', { name: 'Close' })
      );
      await cancelButton.click();

      // Modal should be closed
      await expect(modal).not.toBeVisible();

      // Should still be on packages list
      await expect(page).toHaveURL(/\/packages$/);
    });
  });

  test.describe('Read Package List', () => {
    test('should display packages list', async ({ page }) => {
      const packagesPage = new PackagesPage(page);

      await packagesPage.navigate();
      await packagesPage.waitForPackagesLoaded();

      // Page should have loaded successfully
      await expect(page).toHaveURL(/\/packages/);

      // Create button should be visible
      await expect(packagesPage.createButton).toBeVisible();

      // Search input should be visible
      await expect(packagesPage.searchInput).toBeVisible();
    });

    test('should verify pagination controls', async ({ page }) => {
      const packagesPage = new PackagesPage(page);

      await packagesPage.navigate();
      await packagesPage.waitForPackagesLoaded();

      // Check if pagination exists (may not be visible with few packages)
      const pagination = packagesPage.pagination;

      // If we have pagination, verify it has expected controls
      if (await pagination.isVisible()) {
        // Should have page numbers or navigation
        const pageButtons = pagination.getByRole('button');
        await expect(pageButtons.first()).toBeVisible();
      }
    });

    test('should search packages by name', async ({ page }) => {
      const packagesPage = new PackagesPage(page);

      // First create a package to search for
      const packageData = testData.createPackage();

      await packagesPage.navigate();
      await packagesPage.waitForPackagesLoaded();

      // Create a package first
      await packagesPage.clickCreatePackage();
      const modal = page.getByRole('dialog');
      await modal.getByLabel('Name').fill(packageData.name);
      await modal.getByLabel('Spec URL').fill(packageData.specUrl);
      await modal.getByRole('button', { name: 'Create' }).click();
      await expect(page).toHaveURL(/\/packages\/[\w-]+/);

      // Extract ID for cleanup
      const url = page.url();
      const match = url.match(/\/packages\/([\w-]+)/);
      if (match) {
        createdPackageIds.push(match[1]);
      }

      // Go back to packages list
      await packagesPage.navigate();
      await packagesPage.waitForPackagesLoaded();

      // Search for the created package
      await packagesPage.searchPackages(packageData.name);

      // Verify the package appears in search results
      const packageCard = packagesPage.packageCards.filter({ hasText: packageData.name });
      await expect(packageCard).toBeVisible();
    });

    test('should show empty state when no packages match search', async ({ page }) => {
      const packagesPage = new PackagesPage(page);

      await packagesPage.navigate();
      await packagesPage.waitForPackagesLoaded();

      // Search for a non-existent package
      await packagesPage.searchPackages('nonexistent-package-xyz-12345');

      // Should show empty state or no results message
      const emptyMessage = page.getByText(/no packages found/i).or(
        packagesPage.emptyState
      );
      await expect(emptyMessage).toBeVisible();
    });
  });

  test.describe('Read Package Details', () => {
    let testPackageId: string;
    let testPackageData: PackageData;

    test.beforeEach(async ({ page }) => {
      // Create a package to view details
      const packagesPage = new PackagesPage(page);
      testPackageData = testData.createPackage();

      await packagesPage.navigate();
      await packagesPage.waitForPackagesLoaded();
      await packagesPage.clickCreatePackage();

      const modal = page.getByRole('dialog');
      await modal.getByLabel('Name').fill(testPackageData.name);
      await modal.getByLabel('Spec URL').fill(testPackageData.specUrl);
      if (testPackageData.baseUrl) {
        await modal.getByLabel('Base URL').fill(testPackageData.baseUrl);
      }
      if (testPackageData.description) {
        await modal.getByLabel('Description').fill(testPackageData.description);
      }
      await modal.getByRole('button', { name: 'Create' }).click();

      await expect(page).toHaveURL(/\/packages\/[\w-]+/);

      const url = page.url();
      const match = url.match(/\/packages\/([\w-]+)/);
      testPackageId = match ? match[1] : '';
      createdPackageIds.push(testPackageId);
    });

    test('should display package details correctly', async ({ page }) => {
      const detailPage = new PackageDetailPage(page);

      await detailPage.navigateToPackage(testPackageId);
      await detailPage.waitForLoad();

      // Verify package name
      await expect(detailPage.packageName).toContainText(testPackageData.name);

      // Verify spec URL
      await expect(detailPage.specUrl).toContainText(testPackageData.specUrl);

      // Verify action buttons are present
      await expect(detailPage.editButton).toBeVisible();
      await expect(detailPage.runTestsButton).toBeVisible();
    });

    test('should navigate between tabs', async ({ page }) => {
      const detailPage = new PackageDetailPage(page);

      await detailPage.navigateToPackage(testPackageId);
      await detailPage.waitForLoad();

      // Navigate to Scenarios tab
      await detailPage.goToScenariosTab();
      await expect(detailPage.scenariosList.or(page.getByText(/no scenarios/i))).toBeVisible();

      // Navigate to Runs tab
      await detailPage.goToRunsTab();
      await expect(detailPage.recentRuns.or(page.getByText(/no runs/i))).toBeVisible();

      // Navigate to Settings tab
      await detailPage.goToSettingsTab();
      // Settings tab should show some configuration options
      await expect(detailPage.editButton.or(page.getByText(/settings/i))).toBeVisible();
    });

    test('should navigate back to packages list', async ({ page }) => {
      const detailPage = new PackageDetailPage(page);

      await detailPage.navigateToPackage(testPackageId);
      await detailPage.waitForLoad();

      // Go back
      await detailPage.goBack();

      // Should be on packages list
      await expect(page).toHaveURL(/\/packages$/);
    });
  });

  test.describe('Update Package', () => {
    let testPackageId: string;
    let testPackageData: PackageData;

    test.beforeEach(async ({ page }) => {
      // Create a package to update
      const packagesPage = new PackagesPage(page);
      testPackageData = testData.createPackage();

      await packagesPage.navigate();
      await packagesPage.waitForPackagesLoaded();
      await packagesPage.clickCreatePackage();

      const modal = page.getByRole('dialog');
      await modal.getByLabel('Name').fill(testPackageData.name);
      await modal.getByLabel('Spec URL').fill(testPackageData.specUrl);
      await modal.getByRole('button', { name: 'Create' }).click();

      await expect(page).toHaveURL(/\/packages\/[\w-]+/);

      const url = page.url();
      const match = url.match(/\/packages\/([\w-]+)/);
      testPackageId = match ? match[1] : '';
      createdPackageIds.push(testPackageId);
    });

    test('should open edit modal and update package', async ({ page }) => {
      const detailPage = new PackageDetailPage(page);

      await detailPage.navigateToPackage(testPackageId);
      await detailPage.waitForLoad();

      // Click edit button
      await detailPage.edit();

      // Wait for edit modal/form
      const editModal = page.getByRole('dialog');
      await expect(editModal).toBeVisible();

      // Update name
      const updatedName = `Updated ${testPackageData.name}`;
      await editModal.getByLabel('Name').clear();
      await editModal.getByLabel('Name').fill(updatedName);

      // Save changes
      const saveButton = editModal.getByRole('button', { name: 'Save' }).or(
        editModal.getByRole('button', { name: 'Update' })
      );
      await saveButton.click();

      // Modal should close
      await expect(editModal).not.toBeVisible();

      // Verify update was applied
      await expect(page.getByText(updatedName)).toBeVisible();
    });

    test('should cancel edit without saving changes', async ({ page }) => {
      const detailPage = new PackageDetailPage(page);

      await detailPage.navigateToPackage(testPackageId);
      await detailPage.waitForLoad();

      // Get original name
      const originalName = await detailPage.getName();

      // Click edit button
      await detailPage.edit();

      const editModal = page.getByRole('dialog');
      await expect(editModal).toBeVisible();

      // Make changes
      await editModal.getByLabel('Name').clear();
      await editModal.getByLabel('Name').fill('Should not be saved');

      // Cancel
      const cancelButton = editModal.getByRole('button', { name: 'Cancel' }).or(
        editModal.getByRole('button', { name: 'Close' })
      );
      await cancelButton.click();

      // Modal should close
      await expect(editModal).not.toBeVisible();

      // Original name should still be displayed
      await expect(page.getByText(originalName)).toBeVisible();
    });
  });

  test.describe('Delete Package', () => {
    let testPackageId: string;

    test.beforeEach(async ({ page }) => {
      // Create a package to delete
      const packagesPage = new PackagesPage(page);
      const packageData = testData.createPackage();

      await packagesPage.navigate();
      await packagesPage.waitForPackagesLoaded();
      await packagesPage.clickCreatePackage();

      const modal = page.getByRole('dialog');
      await modal.getByLabel('Name').fill(packageData.name);
      await modal.getByLabel('Spec URL').fill(packageData.specUrl);
      await modal.getByRole('button', { name: 'Create' }).click();

      await expect(page).toHaveURL(/\/packages\/[\w-]+/);

      const url = page.url();
      const match = url.match(/\/packages\/([\w-]+)/);
      testPackageId = match ? match[1] : '';
      // Don't add to cleanup list - we're testing delete
    });

    test('should delete package after confirmation', async ({ page }) => {
      const detailPage = new PackageDetailPage(page);

      await detailPage.navigateToPackage(testPackageId);
      await detailPage.waitForLoad();

      // Click delete button
      await detailPage.clickDelete();

      // Confirm deletion in dialog
      const confirmDialog = page.getByRole('alertdialog').or(page.getByRole('dialog'));
      await expect(confirmDialog).toBeVisible();

      const confirmButton = confirmDialog.getByRole('button', { name: 'Delete' }).or(
        confirmDialog.getByRole('button', { name: 'Confirm' })
      );
      await confirmButton.click();

      // Should navigate to packages list
      await expect(page).toHaveURL(/\/packages$/);

      // Package should not be in list
      const packagesPage = new PackagesPage(page);
      await packagesPage.waitForPackagesLoaded();

      // Verify package was deleted - should not find it
      const packageExists = await packagesPage.packageExists(testPackageId);
      expect(packageExists).toBe(false);
    });

    test('should cancel delete and keep package', async ({ page }) => {
      const detailPage = new PackageDetailPage(page);

      await detailPage.navigateToPackage(testPackageId);
      await detailPage.waitForLoad();

      // Get package name for verification
      const packageName = await detailPage.getName();

      // Click delete button
      await detailPage.clickDelete();

      // Cancel deletion
      const confirmDialog = page.getByRole('alertdialog').or(page.getByRole('dialog'));
      await expect(confirmDialog).toBeVisible();

      const cancelButton = confirmDialog.getByRole('button', { name: 'Cancel' }).or(
        confirmDialog.getByRole('button', { name: 'No' })
      );
      await cancelButton.click();

      // Dialog should close
      await expect(confirmDialog).not.toBeVisible();

      // Should still be on detail page with package visible
      await expect(page.getByText(packageName)).toBeVisible();

      // Add to cleanup since we didn't delete
      createdPackageIds.push(testPackageId);
    });
  });
});
