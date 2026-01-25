import { expect, Locator, Page } from '@playwright/test';

/**
 * Custom assertion helpers for E2E tests
 */

/**
 * Assert element is visible and contains text
 */
export async function assertVisibleWithText(
  locator: Locator,
  expectedText: string
): Promise<void> {
  await expect(locator).toBeVisible();
  await expect(locator).toContainText(expectedText);
}

/**
 * Assert element count
 */
export async function assertCount(
  locator: Locator,
  expectedCount: number
): Promise<void> {
  await expect(locator).toHaveCount(expectedCount);
}

/**
 * Assert URL matches pattern
 */
export async function assertUrl(
  page: Page,
  urlPattern: string | RegExp
): Promise<void> {
  await expect(page).toHaveURL(urlPattern);
}

/**
 * Assert toast notification appears
 */
export async function assertToast(
  page: Page,
  message: string,
  type?: 'success' | 'error' | 'warning' | 'info'
): Promise<void> {
  const toast = page.getByRole('alert');
  await expect(toast).toBeVisible();
  await expect(toast).toContainText(message);

  if (type) {
    await expect(toast).toHaveAttribute('data-type', type);
  }
}

/**
 * Assert form validation error
 */
export async function assertValidationError(
  locator: Locator,
  errorMessage: string
): Promise<void> {
  await expect(locator).toBeVisible();
  await expect(locator).toHaveText(errorMessage);
}

/**
 * Assert page title
 */
export async function assertTitle(
  page: Page,
  expectedTitle: string
): Promise<void> {
  await expect(page).toHaveTitle(expectedTitle);
}

/**
 * Assert element has specific CSS class
 */
export async function assertHasClass(
  locator: Locator,
  className: string
): Promise<void> {
  await expect(locator).toHaveClass(new RegExp(className));
}

/**
 * Assert element is disabled
 */
export async function assertDisabled(locator: Locator): Promise<void> {
  await expect(locator).toBeDisabled();
}

/**
 * Assert element is enabled
 */
export async function assertEnabled(locator: Locator): Promise<void> {
  await expect(locator).toBeEnabled();
}

/**
 * Assert API response status and body
 */
export function assertApiResponse<T>(
  response: { status: number; body: T },
  expectedStatus: number,
  bodyValidator?: (body: T) => boolean
): void {
  expect(response.status).toBe(expectedStatus);
  if (bodyValidator) {
    expect(bodyValidator(response.body)).toBe(true);
  }
}
