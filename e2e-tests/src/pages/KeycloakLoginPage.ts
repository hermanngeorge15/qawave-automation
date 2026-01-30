import { Page, Locator } from '@playwright/test';

/**
 * Keycloak Login Page Object Model
 *
 * Handles interactions with the Keycloak login page during E2E tests.
 * This page is external to the application but part of the auth flow.
 */
export class KeycloakLoginPage {
  readonly page: Page;

  // Form elements
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;

  // Error elements
  readonly errorMessage: Locator;
  readonly invalidCredentialsError: Locator;

  // Additional elements
  readonly forgotPasswordLink: Locator;
  readonly registerLink: Locator;
  readonly rememberMeCheckbox: Locator;

  constructor(page: Page) {
    this.page = page;

    // Standard Keycloak login form selectors
    this.usernameInput = page.locator('#username');
    this.passwordInput = page.locator('#password');
    this.loginButton = page.locator('#kc-login');

    // Error messages
    this.errorMessage = page.locator('.kc-feedback-text');
    this.invalidCredentialsError = page.locator('.alert-error');

    // Optional elements (may not be present depending on Keycloak config)
    this.forgotPasswordLink = page.locator('a[href*="forgot-credentials"]');
    this.registerLink = page.locator('a[href*="registration"]');
    this.rememberMeCheckbox = page.locator('#rememberMe');
  }

  /**
   * Check if we're on the Keycloak login page
   */
  async isOnLoginPage(): Promise<boolean> {
    // Wait briefly for potential redirect
    await this.page.waitForLoadState('networkidle');
    const url = this.page.url();
    return url.includes('/auth/') || url.includes('/realms/');
  }

  /**
   * Wait for the login page to load
   */
  async waitForLoginPage(): Promise<void> {
    await this.usernameInput.waitFor({ state: 'visible', timeout: 30000 });
  }

  /**
   * Fill in login credentials
   */
  async fillCredentials(username: string, password: string): Promise<void> {
    await this.usernameInput.fill(username);
    await this.passwordInput.fill(password);
  }

  /**
   * Click the login button
   */
  async clickLogin(): Promise<void> {
    await this.loginButton.click();
  }

  /**
   * Complete login flow with credentials
   */
  async login(username: string, password: string): Promise<void> {
    await this.waitForLoginPage();
    await this.fillCredentials(username, password);
    await this.clickLogin();
  }

  /**
   * Check if login error is displayed
   */
  async hasError(): Promise<boolean> {
    const feedbackVisible = await this.errorMessage.isVisible();
    const alertVisible = await this.invalidCredentialsError.isVisible();
    return feedbackVisible || alertVisible;
  }

  /**
   * Get error message text
   */
  async getErrorMessage(): Promise<string | null> {
    if (await this.errorMessage.isVisible()) {
      return this.errorMessage.textContent();
    }
    if (await this.invalidCredentialsError.isVisible()) {
      return this.invalidCredentialsError.textContent();
    }
    return null;
  }

  /**
   * Check "Remember Me" checkbox if available
   */
  async checkRememberMe(): Promise<void> {
    if (await this.rememberMeCheckbox.isVisible()) {
      await this.rememberMeCheckbox.check();
    }
  }

  /**
   * Get current page URL
   */
  getCurrentUrl(): string {
    return this.page.url();
  }
}
