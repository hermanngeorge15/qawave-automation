import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Test run detail page object model
 */
export class RunDetailPage extends BasePage {
  readonly url = '/runs'; // Will be /runs/:id

  // Locators
  readonly runStatus: Locator;
  readonly runDuration: Locator;
  readonly runStartTime: Locator;
  readonly runEndTime: Locator;
  readonly testResults: Locator;
  readonly passedTests: Locator;
  readonly failedTests: Locator;
  readonly skippedTests: Locator;
  readonly progressBar: Locator;
  readonly loadingSpinner: Locator;
  readonly backButton: Locator;
  readonly rerunButton: Locator;
  readonly downloadReportButton: Locator;
  readonly testCaseList: Locator;
  readonly testCaseRows: Locator;
  readonly errorDetails: Locator;

  constructor(page: Page) {
    super(page);
    this.runStatus = page.getByTestId('run-status');
    this.runDuration = page.getByTestId('run-duration');
    this.runStartTime = page.getByTestId('run-start-time');
    this.runEndTime = page.getByTestId('run-end-time');
    this.testResults = page.getByTestId('test-results');
    this.passedTests = page.getByTestId('passed-count');
    this.failedTests = page.getByTestId('failed-count');
    this.skippedTests = page.getByTestId('skipped-count');
    this.progressBar = page.getByTestId('progress-bar');
    this.loadingSpinner = page.getByTestId('loading-spinner');
    this.backButton = page.getByRole('button', { name: 'Back' });
    this.rerunButton = page.getByRole('button', { name: 'Re-run' });
    this.downloadReportButton = page.getByRole('button', { name: 'Download Report' });
    this.testCaseList = page.getByTestId('test-case-list');
    this.testCaseRows = page.getByTestId('test-case-row');
    this.errorDetails = page.getByTestId('error-details');
  }

  /**
   * Navigate to specific run by ID
   */
  async navigateToRun(runId: string): Promise<void> {
    await this.page.goto(`/runs/${runId}`);
  }

  /**
   * Wait for run details to load
   */
  async waitForLoad(): Promise<void> {
    await this.loadingSpinner.waitFor({ state: 'hidden', timeout: 30000 });
  }

  /**
   * Get run status text
   */
  async getStatus(): Promise<string> {
    return this.runStatus.textContent() as Promise<string>;
  }

  /**
   * Get passed test count
   */
  async getPassedCount(): Promise<number> {
    const text = await this.passedTests.textContent();
    return parseInt(text || '0', 10);
  }

  /**
   * Get failed test count
   */
  async getFailedCount(): Promise<number> {
    const text = await this.failedTests.textContent();
    return parseInt(text || '0', 10);
  }

  /**
   * Get skipped test count
   */
  async getSkippedCount(): Promise<number> {
    const text = await this.skippedTests.textContent();
    return parseInt(text || '0', 10);
  }

  /**
   * Get total test count
   */
  async getTotalCount(): Promise<number> {
    return this.testCaseRows.count();
  }

  /**
   * Wait for run to complete (status changes from running)
   */
  async waitForRunComplete(timeout = 300000): Promise<void> {
    await expect(this.runStatus).not.toHaveText('Running', { timeout });
  }

  /**
   * Click re-run button
   */
  async rerun(): Promise<void> {
    await this.rerunButton.click();
  }

  /**
   * Click download report button
   */
  async downloadReport(): Promise<void> {
    await this.downloadReportButton.click();
  }

  /**
   * Get failed test details
   */
  async getFailedTestDetails(): Promise<string[]> {
    const failedRows = this.testCaseRows.filter({
      has: this.page.locator('[data-status="failed"]'),
    });
    const count = await failedRows.count();
    const details: string[] = [];
    for (let i = 0; i < count; i++) {
      const row = failedRows.nth(i);
      const text = await row.textContent();
      if (text) details.push(text);
    }
    return details;
  }

  /**
   * Go back to previous page
   */
  async goBack(): Promise<void> {
    await this.backButton.click();
  }

  /**
   * Get run ID from URL
   */
  async getRunIdFromUrl(): Promise<string> {
    const url = this.page.url();
    const match = url.match(/\/runs\/([^/?]+)/);
    return match ? match[1] : '';
  }
}
