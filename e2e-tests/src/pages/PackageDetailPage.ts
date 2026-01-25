import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Package detail page object model
 */
export class PackageDetailPage extends BasePage {
  readonly url = '/packages'; // Will be /packages/:id

  // Locators
  readonly packageName: Locator;
  readonly packageDescription: Locator;
  readonly specUrl: Locator;
  readonly baseUrl: Locator;
  readonly editButton: Locator;
  readonly deleteButton: Locator;
  readonly runTestsButton: Locator;
  readonly scenariosList: Locator;
  readonly scenarioCards: Locator;
  readonly recentRuns: Locator;
  readonly loadingSpinner: Locator;
  readonly backButton: Locator;
  readonly tabScenarios: Locator;
  readonly tabRuns: Locator;
  readonly tabSettings: Locator;

  constructor(page: Page) {
    super(page);
    this.packageName = page.getByTestId('package-name');
    this.packageDescription = page.getByTestId('package-description');
    this.specUrl = page.getByTestId('spec-url');
    this.baseUrl = page.getByTestId('base-url');
    this.editButton = page.getByRole('button', { name: 'Edit' });
    this.deleteButton = page.getByRole('button', { name: 'Delete' });
    this.runTestsButton = page.getByRole('button', { name: 'Run Tests' });
    this.scenariosList = page.getByTestId('scenarios-list');
    this.scenarioCards = page.getByTestId('scenario-card');
    this.recentRuns = page.getByTestId('recent-runs');
    this.loadingSpinner = page.getByTestId('loading-spinner');
    this.backButton = page.getByRole('button', { name: 'Back' });
    this.tabScenarios = page.getByRole('tab', { name: 'Scenarios' });
    this.tabRuns = page.getByRole('tab', { name: 'Runs' });
    this.tabSettings = page.getByRole('tab', { name: 'Settings' });
  }

  /**
   * Navigate to specific package by ID
   */
  async navigateToPackage(packageId: string): Promise<void> {
    await this.page.goto(`/packages/${packageId}`);
  }

  /**
   * Wait for package details to load
   */
  async waitForLoad(): Promise<void> {
    await this.loadingSpinner.waitFor({ state: 'hidden', timeout: 30000 });
  }

  /**
   * Get package name text
   */
  async getName(): Promise<string> {
    return this.packageName.textContent() as Promise<string>;
  }

  /**
   * Get package description text
   */
  async getDescription(): Promise<string> {
    return this.packageDescription.textContent() as Promise<string>;
  }

  /**
   * Click run tests button
   */
  async runTests(): Promise<void> {
    await this.runTestsButton.click();
  }

  /**
   * Click edit button
   */
  async edit(): Promise<void> {
    await this.editButton.click();
  }

  /**
   * Click delete button
   */
  async clickDelete(): Promise<void> {
    await this.deleteButton.click();
  }

  /**
   * Get scenario count
   */
  async getScenarioCount(): Promise<number> {
    return this.scenarioCards.count();
  }

  /**
   * Select a scenario by name
   */
  async selectScenario(name: string): Promise<void> {
    await this.scenarioCards.filter({ hasText: name }).click();
  }

  /**
   * Navigate to scenarios tab
   */
  async goToScenariosTab(): Promise<void> {
    await this.tabScenarios.click();
  }

  /**
   * Navigate to runs tab
   */
  async goToRunsTab(): Promise<void> {
    await this.tabRuns.click();
  }

  /**
   * Navigate to settings tab
   */
  async goToSettingsTab(): Promise<void> {
    await this.tabSettings.click();
  }

  /**
   * Go back to packages list
   */
  async goBack(): Promise<void> {
    await this.backButton.click();
  }

  /**
   * Get package ID from URL
   */
  async getPackageIdFromUrl(): Promise<string> {
    const url = this.page.url();
    const match = url.match(/\/packages\/([^/?]+)/);
    return match ? match[1] : '';
  }
}
