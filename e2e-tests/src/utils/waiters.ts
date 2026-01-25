import { Page, Locator } from '@playwright/test';

/**
 * Wait utilities for E2E tests
 */

/**
 * Wait for loading spinner to disappear
 */
export async function waitForLoadingComplete(
  page: Page,
  timeout = 30000
): Promise<void> {
  const spinner = page.getByTestId('loading-spinner');
  await spinner.waitFor({ state: 'hidden', timeout });
}

/**
 * Wait for element to contain text
 */
export async function waitForText(
  locator: Locator,
  text: string,
  timeout = 10000
): Promise<void> {
  await locator.filter({ hasText: text }).waitFor({ state: 'visible', timeout });
}

/**
 * Wait for network request to complete
 */
export async function waitForApiResponse(
  page: Page,
  urlPattern: string | RegExp,
  timeout = 30000
): Promise<void> {
  await page.waitForResponse(
    (response) => {
      const url = response.url();
      if (typeof urlPattern === 'string') {
        return url.includes(urlPattern);
      }
      return urlPattern.test(url);
    },
    { timeout }
  );
}

/**
 * Wait for multiple API requests to complete
 */
export async function waitForMultipleApiResponses(
  page: Page,
  urlPatterns: (string | RegExp)[],
  timeout = 30000
): Promise<void> {
  await Promise.all(
    urlPatterns.map((pattern) => waitForApiResponse(page, pattern, timeout))
  );
}

/**
 * Wait for page navigation
 */
export async function waitForNavigation(
  page: Page,
  urlPattern: string | RegExp,
  timeout = 30000
): Promise<void> {
  await page.waitForURL(urlPattern, { timeout });
}

/**
 * Wait for animation to complete
 */
export async function waitForAnimation(
  locator: Locator,
  timeout = 5000
): Promise<void> {
  // Wait for element to be stable (no animations)
  await locator.evaluate((el) => {
    return new Promise<void>((resolve) => {
      const checkAnimation = () => {
        const animations = el.getAnimations();
        if (animations.length === 0) {
          resolve();
        } else {
          requestAnimationFrame(checkAnimation);
        }
      };
      checkAnimation();
    });
  });
}

/**
 * Retry action until success
 */
export async function retry<T>(
  action: () => Promise<T>,
  maxRetries = 3,
  delay = 1000
): Promise<T> {
  let lastError: Error | undefined;

  for (let i = 0; i < maxRetries; i++) {
    try {
      return await action();
    } catch (error) {
      lastError = error as Error;
      if (i < maxRetries - 1) {
        await new Promise((resolve) => setTimeout(resolve, delay));
      }
    }
  }

  throw lastError;
}

/**
 * Wait for condition to be true
 */
export async function waitForCondition(
  condition: () => Promise<boolean>,
  timeout = 10000,
  pollInterval = 100
): Promise<void> {
  const startTime = Date.now();

  while (Date.now() - startTime < timeout) {
    if (await condition()) {
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, pollInterval));
  }

  throw new Error(`Condition not met within ${timeout}ms`);
}
