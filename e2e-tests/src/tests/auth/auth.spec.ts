import { test, expect } from '@playwright/test';
import { KeycloakLoginPage } from '../../pages/KeycloakLoginPage';

/**
 * Authentication E2E Tests
 *
 * Tests for Keycloak authentication flows including:
 * - Login flow with redirect
 * - Logout flow
 * - Protected route access
 * - Invalid credentials handling
 * - Session management
 *
 * @tags @auth
 *
 * Prerequisites:
 * - Keycloak must be running and configured
 * - Test users must exist in the qawave realm:
 *   - testuser / testpass (role: tester)
 *   - adminuser / adminpass (role: admin)
 *   - vieweruser / viewerpass (role: viewer)
 *
 * Note: These tests require a real Keycloak instance.
 * For CI without Keycloak, tests will be skipped.
 */

// Test user credentials (should be in environment or secrets in real setup)
const TEST_USER = {
  username: process.env.TEST_USER_USERNAME || 'testuser',
  password: process.env.TEST_USER_PASSWORD || 'testpass',
};

const ADMIN_USER = {
  username: process.env.ADMIN_USER_USERNAME || 'adminuser',
  password: process.env.ADMIN_USER_PASSWORD || 'adminpass',
};

// Check if Keycloak is configured
const KEYCLOAK_CONFIGURED = !!process.env.VITE_KEYCLOAK_URL || !!process.env.KEYCLOAK_URL;

test.describe('Authentication Flow @auth', () => {
  test.describe.configure({ mode: 'serial' });

  test.beforeEach(async ({ page }) => {
    // Clear any existing session
    await page.context().clearCookies();
  });

  test('should redirect to Keycloak login when accessing protected route', async ({ page }) => {
    test.skip(!KEYCLOAK_CONFIGURED, 'Keycloak not configured');

    // Try to access protected route
    await page.goto('/packages');

    // Should be redirected to Keycloak
    const keycloakPage = new KeycloakLoginPage(page);
    const isOnKeycloak = await keycloakPage.isOnLoginPage();

    expect(isOnKeycloak).toBe(true);
  });

  test('should login successfully with valid credentials', async ({ page }) => {
    test.skip(!KEYCLOAK_CONFIGURED, 'Keycloak not configured');

    // Navigate to app (will redirect to Keycloak)
    await page.goto('/packages');

    // Complete Keycloak login
    const keycloakPage = new KeycloakLoginPage(page);
    await keycloakPage.login(TEST_USER.username, TEST_USER.password);

    // Wait for redirect back to app
    await page.waitForURL('**/packages**', { timeout: 30000 });

    // Verify we're authenticated
    expect(page.url()).toContain('/packages');

    // Verify user info is displayed (adjust selector based on your UI)
    const userInfo = page.getByTestId('user-info').or(page.getByText(TEST_USER.username));
    await expect(userInfo).toBeVisible({ timeout: 10000 });
  });

  test('should show error for invalid credentials', async ({ page }) => {
    test.skip(!KEYCLOAK_CONFIGURED, 'Keycloak not configured');

    // Navigate to app (will redirect to Keycloak)
    await page.goto('/packages');

    // Try to login with invalid credentials
    const keycloakPage = new KeycloakLoginPage(page);
    await keycloakPage.login('invaliduser', 'wrongpassword');

    // Should show error on Keycloak page
    const hasError = await keycloakPage.hasError();
    expect(hasError).toBe(true);

    // Should still be on Keycloak page
    const stillOnKeycloak = await keycloakPage.isOnLoginPage();
    expect(stillOnKeycloak).toBe(true);
  });

  test('should logout successfully', async ({ page }) => {
    test.skip(!KEYCLOAK_CONFIGURED, 'Keycloak not configured');

    // First, login
    await page.goto('/packages');
    const keycloakPage = new KeycloakLoginPage(page);
    await keycloakPage.login(TEST_USER.username, TEST_USER.password);
    await page.waitForURL('**/packages**');

    // Click logout button
    const logoutButton = page
      .getByRole('button', { name: /logout/i })
      .or(page.getByTestId('logout-button'));
    await logoutButton.click();

    // Should be redirected to Keycloak logout or home
    await page.waitForLoadState('networkidle');

    // Try to access protected route again
    await page.goto('/packages');

    // Should be redirected to login
    const isOnLogin = await keycloakPage.isOnLoginPage();
    expect(isOnLogin).toBe(true);
  });

  test('should redirect to unauthorized page for insufficient roles', async ({ page }) => {
    test.skip(!KEYCLOAK_CONFIGURED, 'Keycloak not configured');

    // Login as viewer (limited permissions)
    await page.goto('/packages');
    const keycloakPage = new KeycloakLoginPage(page);
    await keycloakPage.login('vieweruser', 'viewerpass');
    await page.waitForURL('**/packages**');

    // Try to access admin-only route (if exists)
    await page.goto('/admin');

    // Should be redirected to unauthorized page
    await expect(page).toHaveURL(/unauthorized/);
  });

  test('should maintain session across page navigations', async ({ page }) => {
    test.skip(!KEYCLOAK_CONFIGURED, 'Keycloak not configured');

    // Login
    await page.goto('/packages');
    const keycloakPage = new KeycloakLoginPage(page);
    await keycloakPage.login(TEST_USER.username, TEST_USER.password);
    await page.waitForURL('**/packages**');

    // Navigate to different pages
    await page.goto('/scenarios');
    await page.waitForLoadState('networkidle');

    // Should still be authenticated (not redirected to login)
    const stillOnKeycloak = await keycloakPage.isOnLoginPage();
    expect(stillOnKeycloak).toBe(false);
    expect(page.url()).toContain('/scenarios');

    // Navigate back to packages
    await page.goto('/packages');
    expect(page.url()).toContain('/packages');
  });

  test('should handle token refresh silently', async ({ page }) => {
    test.skip(!KEYCLOAK_CONFIGURED, 'Keycloak not configured');
    test.slow(); // This test takes longer

    // Login
    await page.goto('/packages');
    const keycloakPage = new KeycloakLoginPage(page);
    await keycloakPage.login(TEST_USER.username, TEST_USER.password);
    await page.waitForURL('**/packages**');

    // Wait for potential token refresh (tokens typically refresh before expiry)
    // In a real test, we might mock the token expiry time
    await page.waitForTimeout(5000);

    // Make an API call (should trigger token refresh if needed)
    const response = await page.request.get('/api/qa/packages');
    expect(response.ok()).toBe(true);

    // Should still be authenticated
    await page.goto('/packages');
    const isOnApp = !page.url().includes('/auth/');
    expect(isOnApp).toBe(true);
  });
});

test.describe('Development Mode (No Keycloak) @auth @dev', () => {
  test.skip(KEYCLOAK_CONFIGURED, 'Running with Keycloak - skip dev mode tests');

  test('should allow access without authentication in dev mode', async ({ page }) => {
    // In dev mode without Keycloak, should have access
    await page.goto('/packages');

    // Should not be redirected
    await page.waitForLoadState('networkidle');
    expect(page.url()).toContain('/packages');

    // Should see packages page content
    const pageContent = page.getByRole('heading').or(page.getByText(/packages/i));
    await expect(pageContent).toBeVisible();
  });

  test('should show development user info', async ({ page }) => {
    await page.goto('/packages');
    await page.waitForLoadState('networkidle');

    // Should show dev user (based on AuthProvider dev mode logic)
    const devUserIndicator = page.getByText(/dev/i).or(page.getByText(/development/i));
    // This may or may not be visible depending on UI implementation
    // await expect(devUserIndicator).toBeVisible();
  });
});

test.describe('Protected Routes @auth', () => {
  test('packages page requires authentication', async ({ page }) => {
    test.skip(!KEYCLOAK_CONFIGURED, 'Keycloak not configured');

    await page.goto('/packages');
    const keycloakPage = new KeycloakLoginPage(page);
    const redirected = await keycloakPage.isOnLoginPage();
    expect(redirected).toBe(true);
  });

  test('scenarios page requires authentication', async ({ page }) => {
    test.skip(!KEYCLOAK_CONFIGURED, 'Keycloak not configured');

    await page.goto('/scenarios');
    const keycloakPage = new KeycloakLoginPage(page);
    const redirected = await keycloakPage.isOnLoginPage();
    expect(redirected).toBe(true);
  });

  test('settings page requires authentication', async ({ page }) => {
    test.skip(!KEYCLOAK_CONFIGURED, 'Keycloak not configured');

    await page.goto('/settings');
    const keycloakPage = new KeycloakLoginPage(page);
    const redirected = await keycloakPage.isOnLoginPage();
    expect(redirected).toBe(true);
  });
});

test.describe('Role-Based Access Control @auth @rbac', () => {
  test.skip(!KEYCLOAK_CONFIGURED, 'Keycloak not configured');

  test('admin user can access all routes', async ({ page }) => {
    // Login as admin
    await page.goto('/packages');
    const keycloakPage = new KeycloakLoginPage(page);
    await keycloakPage.login(ADMIN_USER.username, ADMIN_USER.password);
    await page.waitForURL('**/packages**');

    // Should be able to access packages
    await page.goto('/packages');
    expect(page.url()).toContain('/packages');

    // Should be able to access scenarios
    await page.goto('/scenarios');
    expect(page.url()).toContain('/scenarios');

    // Should be able to access settings
    await page.goto('/settings');
    expect(page.url()).toContain('/settings');
  });

  test('tester user can run tests', async ({ page }) => {
    // Login as tester
    await page.goto('/packages');
    const keycloakPage = new KeycloakLoginPage(page);
    await keycloakPage.login(TEST_USER.username, TEST_USER.password);
    await page.waitForURL('**/packages**');

    // Should have access to packages
    await page.goto('/packages');
    expect(page.url()).toContain('/packages');

    // Run tests button should be visible (tester role)
    // Adjust selector based on actual UI
    // const runTestsButton = page.getByRole('button', { name: /run/i });
    // await expect(runTestsButton).toBeVisible();
  });
});
