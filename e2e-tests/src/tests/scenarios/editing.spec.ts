import { test, expect } from '@playwright/test';
import { ScenarioDetailPage } from '../../pages/ScenarioDetailPage';
import { ScenariosPage } from '../../pages/ScenariosPage';
import { testData } from '../../fixtures/testData';

/**
 * E2E tests for scenario editing workflow
 *
 * Tests cover:
 * - Opening scenario detail page
 * - Editing scenario JSON in editor
 * - Validation errors display
 * - Save/Cancel operations
 * - Undo/Redo functionality
 * - Keyboard shortcuts
 *
 * Note: Some tests are skipped until ENH-001 (Backend API) and ENH-002 (Frontend UI)
 * are implemented.
 *
 * @tags @scenario-editing
 */
test.describe('Scenario Editing Workflow', () => {
  let scenariosPage: ScenariosPage;
  let detailPage: ScenarioDetailPage;

  test.beforeEach(async ({ page }) => {
    scenariosPage = new ScenariosPage(page);
    detailPage = new ScenarioDetailPage(page);
  });

  test.describe('View Scenario Detail', () => {
    test('should open scenario detail page from list', async ({ page }) => {
      // Navigate to scenarios list
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      // Get scenario count - skip if no scenarios
      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      // Click first scenario
      const firstCard = scenariosPage.getScenarioCardByIndex(0);
      await firstCard.click();

      // Verify navigation to detail page
      await expect(page).toHaveURL(/\/scenarios\/[\w-]+/);

      // Verify detail page loaded
      await detailPage.waitForLoad();
      await expect(detailPage.scenarioName).toBeVisible();
    });

    test('should display scenario metadata correctly', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      // Click first scenario
      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();

      // Verify metadata is displayed
      await expect(detailPage.scenarioName).toBeVisible();
      await expect(detailPage.statusBadge).toBeVisible();
      await expect(detailPage.stepCount).toBeVisible();
    });

    test('should display steps tab by default', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();

      // Steps tab should be active by default
      await expect(detailPage.tabSteps).toHaveAttribute('class', /border-primary/);
    });

    test('should switch between tabs', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();

      // Switch to JSON tab
      await detailPage.goToJsonTab();
      await expect(detailPage.tabJson).toHaveAttribute('class', /border-primary/);

      // Switch to History tab
      await detailPage.goToHistoryTab();
      await expect(detailPage.tabHistory).toHaveAttribute('class', /border-primary/);

      // Switch back to Steps tab
      await detailPage.goToStepsTab();
      await expect(detailPage.tabSteps).toHaveAttribute('class', /border-primary/);
    });

    test('should display JSON view in JSON tab', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();

      // Go to JSON tab
      await detailPage.goToJsonTab();

      // Verify JSON is displayed
      const jsonContent = await page.locator('pre, code, [class*="json"]').first();
      await expect(jsonContent).toBeVisible();
    });
  });

  test.describe('Edit Scenario JSON', () => {
    // These tests require the editing feature to be implemented
    // Skip until ENH-001 and ENH-002 are complete

    test.skip('should enter edit mode when clicking Edit button', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();

      // Go to JSON tab
      await detailPage.goToJsonTab();

      // Click Edit button
      await detailPage.enterEditMode();

      // Verify edit mode is active
      expect(await detailPage.isInEditMode()).toBe(true);
      await expect(detailPage.saveButton).toBeVisible();
      await expect(detailPage.cancelButton).toBeVisible();
      await expect(detailPage.jsonEditor).toBeVisible();
    });

    test.skip('should allow editing JSON content', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();
      await detailPage.goToJsonTab();
      await detailPage.enterEditMode();

      // Get original content
      const originalContent = await detailPage.getJsonContent();

      // Modify the content
      const modifiedContent = originalContent.replace('"name":', '"name": "Modified Name",');
      await detailPage.setJsonContent(modifiedContent);

      // Verify content changed in editor
      const editorContent = await detailPage.jsonEditor.inputValue();
      expect(editorContent).toContain('Modified Name');
    });
  });

  test.describe('Validation Errors', () => {
    test.skip('should display validation error for invalid JSON syntax', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();
      await detailPage.goToJsonTab();
      await detailPage.enterEditMode();

      // Enter invalid JSON
      await detailPage.setJsonContent('{ invalid json }');

      // Try to validate
      await detailPage.validateJson();

      // Verify validation error is shown
      expect(await detailPage.hasValidationErrors()).toBe(true);
    });

    test.skip('should display validation error for missing required fields', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();
      await detailPage.goToJsonTab();
      await detailPage.enterEditMode();

      // Enter JSON missing required fields
      await detailPage.setJsonContent('{ "id": "test" }');

      // Try to save
      await detailPage.saveChanges();

      // Verify validation error is shown inline
      expect(await detailPage.hasValidationErrors()).toBe(true);
      const errors = await detailPage.getValidationErrors();
      expect(errors.length).toBeGreaterThan(0);
    });

    test.skip('should clear validation errors when fixed', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();
      await detailPage.goToJsonTab();
      await detailPage.enterEditMode();

      // Enter invalid JSON
      await detailPage.setJsonContent('{ invalid }');
      await detailPage.validateJson();
      expect(await detailPage.hasValidationErrors()).toBe(true);

      // Fix the JSON
      await detailPage.setJsonContent('{ "name": "Valid JSON" }');
      await detailPage.validateJson();

      // Errors should be cleared
      expect(await detailPage.hasValidationErrors()).toBe(false);
    });
  });

  test.describe('Save and Cancel Operations', () => {
    test.skip('should save scenario with valid changes', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();
      await detailPage.goToJsonTab();
      await detailPage.enterEditMode();

      // Get original name
      const originalName = await detailPage.getName();

      // Modify the content with valid JSON
      const newName = `${originalName} - Modified`;
      const content = await detailPage.getJsonContent();
      const modifiedContent = content.replace(originalName, newName);
      await detailPage.setJsonContent(modifiedContent);

      // Save changes
      await detailPage.saveChanges();

      // Verify edit mode is closed
      expect(await detailPage.isInEditMode()).toBe(false);

      // Verify changes persisted
      await detailPage.waitForLoad();
      expect(await detailPage.getName()).toContain('Modified');
    });

    test.skip('should cancel edit without saving changes', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();

      const originalName = await detailPage.getName();

      await detailPage.goToJsonTab();
      await detailPage.enterEditMode();

      // Modify the content
      await detailPage.typeInEditor('{ "name": "Should Not Be Saved" }');

      // Cancel edit
      await detailPage.cancelEdit();

      // Verify edit mode is closed
      expect(await detailPage.isInEditMode()).toBe(false);

      // Verify original content is preserved
      expect(await detailPage.getName()).toBe(originalName);
    });

    test.skip('should prompt for confirmation if closing with unsaved changes', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();
      await detailPage.goToJsonTab();
      await detailPage.enterEditMode();

      // Make changes
      await detailPage.typeInEditor('{ "modified": true }');

      // Try to navigate away
      page.on('dialog', async (dialog) => {
        expect(dialog.message()).toContain('unsaved');
        await dialog.dismiss();
      });

      await page.goBack();
    });
  });

  test.describe('Undo/Redo Functionality', () => {
    test.skip('should undo changes with Undo button', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();
      await detailPage.goToJsonTab();
      await detailPage.enterEditMode();

      // Get original content
      const originalContent = await detailPage.jsonEditor.inputValue();

      // Make a change
      await detailPage.typeInEditor('MODIFIED CONTENT');

      // Click Undo
      await detailPage.undo();

      // Verify content reverted
      const revertedContent = await detailPage.jsonEditor.inputValue();
      expect(revertedContent).toBe(originalContent);
    });

    test.skip('should redo changes with Redo button', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();
      await detailPage.goToJsonTab();
      await detailPage.enterEditMode();

      // Make a change
      await detailPage.typeInEditor('MODIFIED CONTENT');
      const modifiedContent = await detailPage.jsonEditor.inputValue();

      // Undo
      await detailPage.undo();

      // Redo
      await detailPage.redo();

      // Verify content is back to modified state
      const redoneContent = await detailPage.jsonEditor.inputValue();
      expect(redoneContent).toBe(modifiedContent);
    });

    test.skip('should support multiple undo operations', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();
      await detailPage.goToJsonTab();
      await detailPage.enterEditMode();

      const originalContent = await detailPage.jsonEditor.inputValue();

      // Make multiple changes
      await detailPage.typeInEditor('Change 1');
      await detailPage.typeInEditor('Change 2');
      await detailPage.typeInEditor('Change 3');

      // Undo all changes
      await detailPage.undo();
      await detailPage.undo();
      await detailPage.undo();

      // Should be back to original
      const finalContent = await detailPage.jsonEditor.inputValue();
      expect(finalContent).toBe(originalContent);
    });
  });

  test.describe('Keyboard Shortcuts', () => {
    test.skip('should undo with Ctrl/Cmd+Z', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();
      await detailPage.goToJsonTab();
      await detailPage.enterEditMode();

      const originalContent = await detailPage.jsonEditor.inputValue();

      // Make a change
      await detailPage.jsonEditor.focus();
      await detailPage.typeInEditor('KEYBOARD TEST');

      // Use keyboard shortcut to undo
      await detailPage.undoWithKeyboard();

      // Verify undo worked
      const revertedContent = await detailPage.jsonEditor.inputValue();
      expect(revertedContent).toBe(originalContent);
    });

    test.skip('should redo with Ctrl/Cmd+Y', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();
      await detailPage.goToJsonTab();
      await detailPage.enterEditMode();

      // Make a change
      await detailPage.jsonEditor.focus();
      await detailPage.typeInEditor('KEYBOARD TEST');
      const modifiedContent = await detailPage.jsonEditor.inputValue();

      // Undo
      await detailPage.undoWithKeyboard();

      // Redo with keyboard
      await detailPage.redoWithKeyboard();

      // Verify redo worked
      const redoneContent = await detailPage.jsonEditor.inputValue();
      expect(redoneContent).toBe(modifiedContent);
    });

    test.skip('should save with Ctrl/Cmd+S', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();
      await detailPage.goToJsonTab();
      await detailPage.enterEditMode();

      // Make a valid change
      const content = await detailPage.jsonEditor.inputValue();
      await detailPage.jsonEditor.focus();

      // Use keyboard shortcut to save
      await detailPage.saveWithKeyboard();

      // Verify edit mode closed (save succeeded)
      // Wait a bit for save to process
      await page.waitForTimeout(1000);
      expect(await detailPage.isInEditMode()).toBe(false);
    });

    test.skip('should format JSON with Ctrl/Cmd+Shift+F', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();
      await detailPage.goToJsonTab();
      await detailPage.enterEditMode();

      // Enter unformatted JSON
      await detailPage.setJsonContent('{"name":"test","value":123}');

      // Format with keyboard
      await detailPage.formatWithKeyboard();

      // Verify JSON is formatted (has newlines)
      const formattedContent = await detailPage.jsonEditor.inputValue();
      expect(formattedContent).toContain('\n');
    });
  });

  test.describe('Run Test from Detail Page', () => {
    test('should have Run Test button visible', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();

      await expect(detailPage.runTestButton).toBeVisible();
    });

    test.skip('should trigger test run when clicking Run Test', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();

      // Click Run Test
      await detailPage.runTest();

      // Verify button state changes to indicate running
      await expect(detailPage.runTestButton).toHaveText(/Running/i);

      // Wait for response
      await page.waitForResponse(
        (response) => response.url().includes('/runs') && response.status() < 400,
        { timeout: 30000 }
      );
    });
  });

  test.describe('Accessibility', () => {
    test('should have proper heading structure', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();

      // Check for h1 heading
      const h1 = page.getByRole('heading', { level: 1 });
      await expect(h1).toBeVisible();
    });

    test('should have accessible tab navigation', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();

      // Tabs should be keyboard navigable
      await detailPage.tabSteps.focus();
      await page.keyboard.press('Tab');
      await page.keyboard.press('Tab');

      // JSON tab should be focusable
      await expect(detailPage.tabJson).toBeFocused();
    });

    test('should have accessible buttons with labels', async ({ page }) => {
      await scenariosPage.navigate();
      await scenariosPage.waitForScenariosLoaded();

      const count = await scenariosPage.getScenarioCount();
      test.skip(count === 0, 'No scenarios available to test');

      await scenariosPage.getScenarioCardByIndex(0).click();
      await detailPage.waitForLoad();

      // Run Test button should have accessible name
      await expect(detailPage.runTestButton).toHaveAccessibleName(/Run Test/i);
    });
  });
});
