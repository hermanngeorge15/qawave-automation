import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Login page object model
 */
export class LoginPage extends BasePage {
  readonly url = '/login';

  // Locators
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly signInButton: Locator;
  readonly errorMessage: Locator;
  readonly forgotPasswordLink: Locator;
  readonly signUpLink: Locator;
  readonly loadingSpinner: Locator;

  constructor(page: Page) {
    super(page);
    this.emailInput = page.getByLabel('Email');
    this.passwordInput = page.getByLabel('Password');
    this.signInButton = page.getByRole('button', { name: 'Sign in' });
    this.errorMessage = page.getByRole('alert');
    this.forgotPasswordLink = page.getByRole('link', { name: 'Forgot password?' });
    this.signUpLink = page.getByRole('link', { name: 'Sign up' });
    this.loadingSpinner = page.getByTestId('loading-spinner');
  }

  /**
   * Fill login form with credentials
   */
  async fillCredentials(email: string, password: string): Promise<void> {
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
  }

  /**
   * Submit login form
   */
  async submit(): Promise<void> {
    await this.signInButton.click();
  }

  /**
   * Complete login flow
   */
  async login(email: string, password: string): Promise<void> {
    await this.fillCredentials(email, password);
    await this.submit();
  }

  /**
   * Wait for login to complete (redirect away from login page)
   */
  async waitForLoginComplete(): Promise<void> {
    await this.page.waitForURL((url) => !url.pathname.includes('/login'));
  }

  /**
   * Get error message text
   */
  async getErrorMessage(): Promise<string> {
    await this.errorMessage.waitFor({ state: 'visible' });
    return this.errorMessage.textContent() as Promise<string>;
  }

  /**
   * Check if login form is visible
   */
  async isFormVisible(): Promise<boolean> {
    return this.signInButton.isVisible();
  }

  /**
   * Navigate to forgot password page
   */
  async goToForgotPassword(): Promise<void> {
    await this.forgotPasswordLink.click();
  }

  /**
   * Navigate to sign up page
   */
  async goToSignUp(): Promise<void> {
    await this.signUpLink.click();
  }
}
