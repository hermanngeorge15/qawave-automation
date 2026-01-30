import { Page, Locator } from '@playwright/test';

/**
 * Data structure for creating a package
 */
export interface PackageFormData {
  name: string;
  specUrl: string;
  baseUrl?: string;
  description?: string;
}

/**
 * Create Package Modal page object model
 */
export class CreatePackageModal {
  readonly page: Page;

  // Modal container
  readonly modal: Locator;

  // Form fields
  readonly nameInput: Locator;
  readonly specUrlInput: Locator;
  readonly baseUrlInput: Locator;
  readonly descriptionInput: Locator;

  // Buttons
  readonly submitButton: Locator;
  readonly cancelButton: Locator;
  readonly closeButton: Locator;

  // Validation errors
  readonly nameError: Locator;
  readonly specUrlError: Locator;
  readonly baseUrlError: Locator;

  // Loading state
  readonly loadingSpinner: Locator;

  constructor(page: Page) {
    this.page = page;
    this.modal = page.getByRole('dialog');

    // Form fields by label
    this.nameInput = this.modal.getByLabel('Name');
    this.specUrlInput = this.modal.getByLabel('Spec URL');
    this.baseUrlInput = this.modal.getByLabel('Base URL');
    this.descriptionInput = this.modal.getByLabel('Description');

    // Buttons
    this.submitButton = this.modal.getByRole('button', { name: 'Create' });
    this.cancelButton = this.modal.getByRole('button', { name: 'Cancel' });
    this.closeButton = this.modal.getByRole('button', { name: 'Close' });

    // Error messages
    this.nameError = this.modal.getByTestId('name-error');
    this.specUrlError = this.modal.getByTestId('spec-url-error');
    this.baseUrlError = this.modal.getByTestId('base-url-error');

    // Loading
    this.loadingSpinner = this.modal.getByTestId('loading-spinner');
  }

  /**
   * Wait for modal to be visible
   */
  async waitForVisible(): Promise<void> {
    await this.modal.waitFor({ state: 'visible' });
  }

  /**
   * Wait for modal to be hidden
   */
  async waitForHidden(): Promise<void> {
    await this.modal.waitFor({ state: 'hidden' });
  }

  /**
   * Check if modal is visible
   */
  async isVisible(): Promise<boolean> {
    return this.modal.isVisible();
  }

  /**
   * Fill the package form with data
   */
  async fillForm(data: PackageFormData): Promise<void> {
    await this.nameInput.fill(data.name);
    await this.specUrlInput.fill(data.specUrl);

    if (data.baseUrl) {
      await this.baseUrlInput.fill(data.baseUrl);
    }

    if (data.description) {
      await this.descriptionInput.fill(data.description);
    }
  }

  /**
   * Clear all form fields
   */
  async clearForm(): Promise<void> {
    await this.nameInput.clear();
    await this.specUrlInput.clear();
    await this.baseUrlInput.clear();
    await this.descriptionInput.clear();
  }

  /**
   * Submit the form
   */
  async submit(): Promise<void> {
    await this.submitButton.click();
  }

  /**
   * Cancel and close modal
   */
  async cancel(): Promise<void> {
    await this.cancelButton.click();
  }

  /**
   * Close modal via X button
   */
  async close(): Promise<void> {
    await this.closeButton.click();
  }

  /**
   * Fill form and submit
   */
  async createPackage(data: PackageFormData): Promise<void> {
    await this.fillForm(data);
    await this.submit();
  }

  /**
   * Check if submit button is enabled
   */
  async isSubmitEnabled(): Promise<boolean> {
    return this.submitButton.isEnabled();
  }

  /**
   * Check if form has validation errors
   */
  async hasValidationErrors(): Promise<boolean> {
    const nameErrorVisible = await this.nameError.isVisible();
    const specUrlErrorVisible = await this.specUrlError.isVisible();
    const baseUrlErrorVisible = await this.baseUrlError.isVisible();
    return nameErrorVisible || specUrlErrorVisible || baseUrlErrorVisible;
  }

  /**
   * Get name validation error text
   */
  async getNameError(): Promise<string | null> {
    if (await this.nameError.isVisible()) {
      return this.nameError.textContent();
    }
    return null;
  }

  /**
   * Get spec URL validation error text
   */
  async getSpecUrlError(): Promise<string | null> {
    if (await this.specUrlError.isVisible()) {
      return this.specUrlError.textContent();
    }
    return null;
  }

  /**
   * Get base URL validation error text
   */
  async getBaseUrlError(): Promise<string | null> {
    if (await this.baseUrlError.isVisible()) {
      return this.baseUrlError.textContent();
    }
    return null;
  }

  /**
   * Wait for form submission to complete (loading spinner hidden)
   */
  async waitForSubmission(): Promise<void> {
    // Wait for spinner to appear and disappear
    await this.loadingSpinner.waitFor({ state: 'visible', timeout: 5000 }).catch(() => {
      // Spinner might not appear if submission is fast
    });
    await this.loadingSpinner.waitFor({ state: 'hidden', timeout: 30000 }).catch(() => {
      // Spinner might already be hidden
    });
  }
}
