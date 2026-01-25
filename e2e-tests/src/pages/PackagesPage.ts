import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Packages list page object model
 */
export class PackagesPage extends BasePage {
  readonly url = '/packages';

  // Locators
  readonly createButton: Locator;
  readonly packageCards: Locator;
  readonly searchInput: Locator;
  readonly loadingSpinner: Locator;
  readonly emptyState: Locator;
  readonly errorMessage: Locator;
  readonly refreshButton: Locator;
  readonly filterDropdown: Locator;
  readonly pagination: Locator;
  readonly sortDropdown: Locator;

  constructor(page: Page) {
    super(page);
    this.createButton = page.getByRole('button', { name: 'Create Package' });
    this.packageCards = page.getByTestId('package-card');
    this.searchInput = page.getByPlaceholder('Search packages...');
    this.loadingSpinner = page.getByTestId('loading-spinner');
    this.emptyState = page.getByTestId('empty-state');
    this.errorMessage = page.getByRole('alert');
    this.refreshButton = page.getByRole('button', { name: 'Refresh' });
    this.filterDropdown = page.getByTestId('filter-dropdown');
    this.pagination = page.getByTestId('pagination');
    this.sortDropdown = page.getByTestId('sort-dropdown');
  }

  /**
   * Wait for packages to finish loading
   */
  async waitForPackagesLoaded(): Promise<void> {
    await this.loadingSpinner.waitFor({ state: 'hidden', timeout: 30000 });
  }

  /**
   * Get count of visible package cards
   */
  async getPackageCount(): Promise<number> {
    await this.waitForPackagesLoaded();
    return this.packageCards.count();
  }

  /**
   * Click create package button
   */
  async clickCreatePackage(): Promise<void> {
    await this.createButton.click();
  }

  /**
   * Search for packages by name
   */
  async searchPackages(query: string): Promise<void> {
    await this.searchInput.fill(query);
    await this.page.keyboard.press('Enter');
    await this.waitForPackagesLoaded();
  }

  /**
   * Clear search input
   */
  async clearSearch(): Promise<void> {
    await this.searchInput.clear();
    await this.page.keyboard.press('Enter');
    await this.waitForPackagesLoaded();
  }

  /**
   * Select a package by name
   */
  async selectPackage(name: string): Promise<void> {
    await this.packageCards.filter({ hasText: name }).click();
  }

  /**
   * Get package card by index
   */
  getPackageCardByIndex(index: number): Locator {
    return this.packageCards.nth(index);
  }

  /**
   * Get package names from visible cards
   */
  async getPackageNames(): Promise<string[]> {
    await this.waitForPackagesLoaded();
    const cards = await this.packageCards.all();
    const names: string[] = [];
    for (const card of cards) {
      const nameElement = card.getByTestId('package-name');
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
   * Refresh the packages list
   */
  async refresh(): Promise<void> {
    await this.refreshButton.click();
    await this.waitForPackagesLoaded();
  }

  /**
   * Check if package with name exists
   */
  async packageExists(name: string): Promise<boolean> {
    const names = await this.getPackageNames();
    return names.includes(name);
  }
}
