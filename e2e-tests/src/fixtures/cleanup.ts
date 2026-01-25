import { request } from '@playwright/test';

const API_URL = process.env.API_URL ?? 'http://localhost:8080';

/**
 * Cleanup helper for test data management
 * Ensures test isolation by removing created resources after tests
 */

/**
 * Delete a QA package by ID
 */
export async function cleanupPackage(id: string): Promise<void> {
  const context = await request.newContext({ baseURL: API_URL });
  try {
    await context.delete(`/api/qa/packages/${id}`);
  } catch (error) {
    console.warn(`Failed to cleanup package ${id}:`, error);
  } finally {
    await context.dispose();
  }
}

/**
 * Delete a scenario by ID
 */
export async function cleanupScenario(id: string): Promise<void> {
  const context = await request.newContext({ baseURL: API_URL });
  try {
    await context.delete(`/api/qa/scenarios/${id}`);
  } catch (error) {
    console.warn(`Failed to cleanup scenario ${id}:`, error);
  } finally {
    await context.dispose();
  }
}

/**
 * Delete a test run by ID
 */
export async function cleanupTestRun(id: string): Promise<void> {
  const context = await request.newContext({ baseURL: API_URL });
  try {
    await context.delete(`/api/qa/runs/${id}`);
  } catch (error) {
    console.warn(`Failed to cleanup test run ${id}:`, error);
  } finally {
    await context.dispose();
  }
}

/**
 * Bulk cleanup multiple resources
 */
export async function cleanupAll(resources: {
  packages?: string[];
  scenarios?: string[];
  testRuns?: string[];
}): Promise<void> {
  const { packages = [], scenarios = [], testRuns = [] } = resources;

  await Promise.all([
    ...packages.map(cleanupPackage),
    ...scenarios.map(cleanupScenario),
    ...testRuns.map(cleanupTestRun),
  ]);
}
