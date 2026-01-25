import { Page, Locator } from '@playwright/test';

/**
 * Base page class with common functionality
 * All page objects should extend this class
 */
export abstract class BasePage {
  constructor(protected page: Page) {}

  /**
   * URL path for this page (override in subclass)
   */
  abstract readonly url: string;

  /**
   * Navigate to this page
   */
  async navigate(): Promise<void> {
    await this.page.goto(this.url);
  }

  /**
   * Wait for page to fully load
   */
  async waitForLoad(): Promise<void> {
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Wait for element to be visible
   */
  async waitForElement(locator: Locator, timeout = 10000): Promise<void> {
    await locator.waitFor({ state: 'visible', timeout });
  }

  /**
   * Wait for element to be hidden
   */
  async waitForElementHidden(locator: Locator, timeout = 10000): Promise<void> {
    await locator.waitFor({ state: 'hidden', timeout });
  }

  /**
   * Get current page URL
   */
  async getCurrentUrl(): Promise<string> {
    return this.page.url();
  }

  /**
   * Get page title
   */
  async getTitle(): Promise<string> {
    return this.page.title();
  }

  /**
   * Take screenshot
   */
  async screenshot(name: string): Promise<void> {
    await this.page.screenshot({ path: `screenshots/${name}.png` });
  }

  /**
   * Wait for navigation to complete
   */
  async waitForNavigation(url?: string | RegExp): Promise<void> {
    if (url) {
      await this.page.waitForURL(url);
    } else {
      await this.page.waitForLoadState('load');
    }
  }

  /**
   * Check if element exists on page
   */
  async elementExists(locator: Locator): Promise<boolean> {
    return (await locator.count()) > 0;
  }

  /**
   * Scroll to element
   */
  async scrollToElement(locator: Locator): Promise<void> {
    await locator.scrollIntoViewIfNeeded();
  }
}
