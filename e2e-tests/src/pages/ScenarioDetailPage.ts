import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Scenario detail page object model
 * Supports viewing and editing scenarios
 */
export class ScenarioDetailPage extends BasePage {
  readonly url = '/scenarios'; // Will be /scenarios/:id

  // Header locators
  readonly scenarioName: Locator;
  readonly scenarioDescription: Locator;
  readonly statusBadge: Locator;
  readonly packageLink: Locator;
  readonly stepCount: Locator;
  readonly createdAt: Locator;

  // Action buttons
  readonly runTestButton: Locator;
  readonly editButton: Locator;
  readonly saveButton: Locator;
  readonly cancelButton: Locator;
  readonly deleteButton: Locator;

  // Tab navigation
  readonly tabSteps: Locator;
  readonly tabHistory: Locator;
  readonly tabJson: Locator;

  // Steps tab
  readonly stepCards: Locator;
  readonly emptyStepsState: Locator;

  // JSON tab / Editor
  readonly jsonViewer: Locator;
  readonly jsonEditor: Locator;
  readonly copyButton: Locator;

  // Editor controls
  readonly undoButton: Locator;
  readonly redoButton: Locator;
  readonly formatButton: Locator;
  readonly validateButton: Locator;

  // Validation
  readonly validationErrors: Locator;
  readonly validationSuccess: Locator;

  // Loading states
  readonly loadingSpinner: Locator;
  readonly savingIndicator: Locator;

  constructor(page: Page) {
    super(page);

    // Header
    this.scenarioName = page.getByRole('heading', { level: 1 });
    this.scenarioDescription = page.locator('.scenario-detail-page p').first();
    this.statusBadge = page.getByTestId('status-badge');
    this.packageLink = page.locator('a[href*="/packages/"]');
    this.stepCount = page.getByText(/\d+ steps/);
    this.createdAt = page.getByText(/Created/);

    // Action buttons
    this.runTestButton = page.getByRole('button', { name: /Run Test/i });
    this.editButton = page.getByRole('button', { name: /Edit/i });
    this.saveButton = page.getByRole('button', { name: /Save/i });
    this.cancelButton = page.getByRole('button', { name: /Cancel/i });
    this.deleteButton = page.getByRole('button', { name: /Delete/i });

    // Tabs
    this.tabSteps = page.getByRole('button', { name: 'Steps' });
    this.tabHistory = page.getByRole('button', { name: 'Run History' });
    this.tabJson = page.getByRole('button', { name: 'JSON' });

    // Steps tab content
    this.stepCards = page.locator('.card').filter({ has: page.locator('[class*="rounded-full"]') });
    this.emptyStepsState = page.getByText('No steps defined');

    // JSON tab / Editor
    this.jsonViewer = page.getByTestId('json-viewer');
    this.jsonEditor = page.getByTestId('json-editor');
    this.copyButton = page.getByRole('button', { name: /Copy/i });

    // Editor controls
    this.undoButton = page.getByRole('button', { name: /Undo/i });
    this.redoButton = page.getByRole('button', { name: /Redo/i });
    this.formatButton = page.getByRole('button', { name: /Format/i });
    this.validateButton = page.getByRole('button', { name: /Validate/i });

    // Validation
    this.validationErrors = page.getByTestId('validation-errors');
    this.validationSuccess = page.getByTestId('validation-success');

    // Loading states
    this.loadingSpinner = page.getByTestId('loading-spinner');
    this.savingIndicator = page.getByText(/Saving/i);
  }

  /**
   * Navigate to specific scenario by ID
   */
  async navigateToScenario(scenarioId: string): Promise<void> {
    await this.page.goto(`/scenarios/${scenarioId}`);
  }

  /**
   * Wait for scenario details to load
   */
  async waitForLoad(): Promise<void> {
    // Wait for either content to load or error to show
    await Promise.race([
      this.scenarioName.waitFor({ state: 'visible', timeout: 30000 }),
      this.page.getByText(/Error loading scenario/i).waitFor({ state: 'visible', timeout: 30000 }),
    ]);
  }

  /**
   * Get scenario name text
   */
  async getName(): Promise<string> {
    return this.scenarioName.textContent() as Promise<string>;
  }

  /**
   * Get scenario status text
   */
  async getStatus(): Promise<string | null> {
    return this.statusBadge.textContent();
  }

  /**
   * Get step count from the UI
   */
  async getStepCount(): Promise<number> {
    const text = await this.stepCount.textContent();
    const match = text?.match(/(\d+) steps/);
    return match ? parseInt(match[1], 10) : 0;
  }

  // Tab navigation methods

  /**
   * Go to Steps tab
   */
  async goToStepsTab(): Promise<void> {
    await this.tabSteps.click();
  }

  /**
   * Go to Run History tab
   */
  async goToHistoryTab(): Promise<void> {
    await this.tabHistory.click();
  }

  /**
   * Go to JSON tab
   */
  async goToJsonTab(): Promise<void> {
    await this.tabJson.click();
  }

  // Edit mode methods

  /**
   * Enter edit mode
   */
  async enterEditMode(): Promise<void> {
    await this.editButton.click();
    // Wait for editor to be visible
    await this.jsonEditor.waitFor({ state: 'visible', timeout: 5000 });
  }

  /**
   * Check if edit mode is active
   */
  async isInEditMode(): Promise<boolean> {
    return this.saveButton.isVisible();
  }

  /**
   * Save changes
   */
  async saveChanges(): Promise<void> {
    await this.saveButton.click();
    // Wait for save to complete
    await this.savingIndicator.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {});
  }

  /**
   * Cancel editing
   */
  async cancelEdit(): Promise<void> {
    await this.cancelButton.click();
  }

  /**
   * Get JSON content from viewer
   */
  async getJsonContent(): Promise<string> {
    return this.jsonViewer.textContent() ?? '';
  }

  /**
   * Set JSON content in editor
   */
  async setJsonContent(content: string): Promise<void> {
    await this.jsonEditor.fill(content);
  }

  /**
   * Clear and type in JSON editor
   */
  async typeInEditor(content: string): Promise<void> {
    await this.jsonEditor.clear();
    await this.jsonEditor.type(content);
  }

  /**
   * Copy JSON to clipboard
   */
  async copyJson(): Promise<void> {
    await this.copyButton.click();
  }

  // Editor actions

  /**
   * Undo last change
   */
  async undo(): Promise<void> {
    await this.undoButton.click();
  }

  /**
   * Redo last undone change
   */
  async redo(): Promise<void> {
    await this.redoButton.click();
  }

  /**
   * Format JSON in editor
   */
  async formatJson(): Promise<void> {
    await this.formatButton.click();
  }

  /**
   * Validate JSON in editor
   */
  async validateJson(): Promise<void> {
    await this.validateButton.click();
  }

  /**
   * Check if validation errors are displayed
   */
  async hasValidationErrors(): Promise<boolean> {
    return this.validationErrors.isVisible();
  }

  /**
   * Get validation error messages
   */
  async getValidationErrors(): Promise<string[]> {
    const errors = await this.validationErrors.allTextContents();
    return errors;
  }

  /**
   * Check if validation passed
   */
  async isValidationSuccessful(): Promise<boolean> {
    return this.validationSuccess.isVisible();
  }

  // Keyboard shortcuts

  /**
   * Use keyboard shortcut for undo (Ctrl/Cmd+Z)
   */
  async undoWithKeyboard(): Promise<void> {
    const modifier = process.platform === 'darwin' ? 'Meta' : 'Control';
    await this.page.keyboard.press(`${modifier}+z`);
  }

  /**
   * Use keyboard shortcut for redo (Ctrl/Cmd+Y or Ctrl/Cmd+Shift+Z)
   */
  async redoWithKeyboard(): Promise<void> {
    const modifier = process.platform === 'darwin' ? 'Meta' : 'Control';
    await this.page.keyboard.press(`${modifier}+y`);
  }

  /**
   * Use keyboard shortcut for save (Ctrl/Cmd+S)
   */
  async saveWithKeyboard(): Promise<void> {
    const modifier = process.platform === 'darwin' ? 'Meta' : 'Control';
    await this.page.keyboard.press(`${modifier}+s`);
  }

  /**
   * Use keyboard shortcut for format (Ctrl/Cmd+Shift+F)
   */
  async formatWithKeyboard(): Promise<void> {
    const modifier = process.platform === 'darwin' ? 'Meta' : 'Control';
    await this.page.keyboard.press(`${modifier}+Shift+f`);
  }

  // Run actions

  /**
   * Run the scenario test
   */
  async runTest(): Promise<void> {
    await this.runTestButton.click();
  }

  /**
   * Check if run test button is enabled
   */
  async isRunTestEnabled(): Promise<boolean> {
    return this.runTestButton.isEnabled();
  }

  // Step inspection

  /**
   * Get step card count
   */
  async getVisibleStepCount(): Promise<number> {
    return this.stepCards.count();
  }

  /**
   * Expand step card by index
   */
  async expandStep(index: number): Promise<void> {
    await this.stepCards.nth(index).click();
  }

  /**
   * Get step method (GET, POST, etc) by index
   */
  async getStepMethod(index: number): Promise<string | null> {
    const step = this.stepCards.nth(index);
    const methodBadge = step.locator('[class*="font-mono"]').first();
    return methodBadge.textContent();
  }

  /**
   * Get step endpoint by index
   */
  async getStepEndpoint(index: number): Promise<string | null> {
    const step = this.stepCards.nth(index);
    const endpoint = step.locator('.font-mono').nth(1);
    return endpoint.textContent();
  }
}
