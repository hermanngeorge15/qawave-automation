import { Page, BrowserContext } from '@playwright/test';

const API_URL = process.env.API_URL ?? 'http://localhost:8080';

/**
 * Authentication helpers for E2E tests
 */

export interface AuthCredentials {
  email: string;
  password: string;
}

/**
 * Login via UI
 */
export async function loginViaUI(
  page: Page,
  credentials: AuthCredentials
): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('Email').fill(credentials.email);
  await page.getByLabel('Password').fill(credentials.password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.waitForURL('/');
}

/**
 * Login via API and set cookies
 */
export async function loginViaAPI(
  context: BrowserContext,
  credentials: AuthCredentials
): Promise<void> {
  const response = await context.request.post(`${API_URL}/api/auth/login`, {
    data: credentials,
  });

  if (!response.ok()) {
    throw new Error(`Login failed: ${response.status()}`);
  }

  // Cookies are automatically stored in the context
}

/**
 * Logout via UI
 */
export async function logoutViaUI(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'User menu' }).click();
  await page.getByRole('menuitem', { name: 'Sign out' }).click();
  await page.waitForURL('/login');
}

/**
 * Check if user is authenticated
 */
export async function isAuthenticated(page: Page): Promise<boolean> {
  try {
    // Check for user menu presence as indicator of auth state
    const userMenu = page.getByRole('button', { name: 'User menu' });
    return await userMenu.isVisible({ timeout: 1000 });
  } catch {
    return false;
  }
}

/**
 * Create authenticated browser context with stored auth state
 */
export async function createAuthenticatedContext(
  context: BrowserContext,
  credentials: AuthCredentials
): Promise<void> {
  await loginViaAPI(context, credentials);
}
