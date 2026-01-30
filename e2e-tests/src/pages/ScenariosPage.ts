import { Page, Locator } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Scenarios list page object model
 */
export class ScenariosPage extends BasePage {
  readonly url = '/scenarios';

  // Locators
  readonly scenarioCards: Locator;
  readonly searchInput: Locator;
  readonly loadingSpinner: Locator;
  readonly emptyState: Locator;
  readonly errorMessage: Locator;
  readonly refreshButton: Locator;
  readonly filterDropdown: Locator;
  readonly pagination: Locator;
  readonly packageFilter: Locator;
  readonly statusFilter: Locator;

  constructor(page: Page) {
    super(page);
    this.scenarioCards = page.getByTestId('scenario-card');
    this.searchInput = page.getByPlaceholder('Search scenarios...');
    this.loadingSpinner = page.getByTestId('loading-spinner');
    this.emptyState = page.getByTestId('empty-state');
    this.errorMessage = page.getByRole('alert');
    this.refreshButton = page.getByRole('button', { name: 'Refresh' });
    this.filterDropdown = page.getByTestId('filter-dropdown');
    this.pagination = page.getByTestId('pagination');
    this.packageFilter = page.getByTestId('package-filter');
    this.statusFilter = page.getByTestId('status-filter');
  }

  /**
   * Wait for scenarios to finish loading
   */
  async waitForScenariosLoaded(): Promise<void> {
    await this.loadingSpinner.waitFor({ state: 'hidden', timeout: 30000 });
  }

  /**
   * Get count of visible scenario cards
   */
  async getScenarioCount(): Promise<number> {
    await this.waitForScenariosLoaded();
    return this.scenarioCards.count();
  }

  /**
   * Search for scenarios by name
   */
  async searchScenarios(query: string): Promise<void> {
    await this.searchInput.fill(query);
    await this.page.keyboard.press('Enter');
    await this.waitForScenariosLoaded();
  }

  /**
   * Clear search input
   */
  async clearSearch(): Promise<void> {
    await this.searchInput.clear();
    await this.page.keyboard.press('Enter');
    await this.waitForScenariosLoaded();
  }

  /**
   * Select a scenario by name
   */
  async selectScenario(name: string): Promise<void> {
    await this.scenarioCards.filter({ hasText: name }).click();
  }

  /**
   * Get scenario card by index
   */
  getScenarioCardByIndex(index: number): Locator {
    return this.scenarioCards.nth(index);
  }

  /**
   * Get scenario names from visible cards
   */
  async getScenarioNames(): Promise<string[]> {
    await this.waitForScenariosLoaded();
    const cards = await this.scenarioCards.all();
    const names: string[] = [];
    for (const card of cards) {
      const nameElement = card.getByTestId('scenario-name');
      const name = await nameElement.textContent();
      if (name) names.push(name);
    }
    return names;
  }

  /**
   * Check if empty state is displayed
   */
  async isEmptyStateVisible(): Promise<boolean> {
    return this.emptyState.isVisible();
  }

  /**
   * Filter scenarios by package
   */
  async filterByPackage(packageName: string): Promise<void> {
    await this.packageFilter.click();
    await this.page.getByRole('option', { name: packageName }).click();
    await this.waitForScenariosLoaded();
  }

  /**
   * Filter scenarios by status
   */
  async filterByStatus(status: 'active' | 'draft' | 'archived'): Promise<void> {
    await this.statusFilter.click();
    await this.page.getByRole('option', { name: status, exact: false }).click();
    await this.waitForScenariosLoaded();
  }

  /**
   * Refresh the scenarios list
   */
  async refresh(): Promise<void> {
    await this.refreshButton.click();
    await this.waitForScenariosLoaded();
  }

  /**
   * Check if scenario with name exists
   */
  async scenarioExists(name: string): Promise<boolean> {
    const names = await this.getScenarioNames();
    return names.includes(name);
  }

  /**
   * Get scenario status by name
   */
  async getScenarioStatus(name: string): Promise<string | null> {
    const card = this.scenarioCards.filter({ hasText: name });
    const status = card.getByTestId('scenario-status');
    return status.textContent();
  }
}
