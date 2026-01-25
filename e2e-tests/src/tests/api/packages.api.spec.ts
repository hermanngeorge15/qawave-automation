import { test, expect } from '@playwright/test';
import { PackagesApi, Package, CreatePackageRequest } from '../../api/packages.api';
import { testData } from '../../fixtures/testData';

/**
 * API tests for Packages endpoints
 * These tests verify the backend API contract
 */
test.describe('Packages API @api', () => {
  let api: PackagesApi;
  let createdPackageIds: string[] = [];

  test.beforeAll(async ({ request }) => {
    api = new PackagesApi(request);
  });

  test.afterAll(async () => {
    // Cleanup all created packages
    for (const id of createdPackageIds) {
      try {
        await api.deletePackage(id);
      } catch {
        // Ignore cleanup errors
      }
    }
  });

  test('GET /api/qa/packages should return paginated list', async () => {
    const response = await api.list();

    expect(response.status).toBe(200);
    expect(response.body).toHaveProperty('content');
    expect(response.body).toHaveProperty('totalElements');
    expect(response.body).toHaveProperty('totalPages');
    expect(response.body).toHaveProperty('size');
    expect(response.body).toHaveProperty('number');
    expect(Array.isArray(response.body.content)).toBe(true);
  });

  test('GET /api/qa/packages should support pagination params', async () => {
    const response = await api.list(0, 5);

    expect(response.status).toBe(200);
    expect(response.body.size).toBe(5);
    expect(response.body.number).toBe(0);
  });

  test('POST /api/qa/packages should create package with valid data', async () => {
    const data = testData.createPackage();
    const response = await api.create(data);

    expect(response.status).toBe(201);
    expect(response.body).toHaveProperty('id');
    expect(response.body.specUrl).toBe(data.specUrl);
    expect(response.body.name).toBe(data.name);
    expect(response.body).toHaveProperty('createdAt');

    // Store for cleanup
    createdPackageIds.push(response.body.id);
  });

  test('POST /api/qa/packages should validate required fields', async () => {
    const response = await api.create({ specUrl: '' } as CreatePackageRequest);

    expect(response.status).toBe(400);
    expect(response.body).toHaveProperty('message');
  });

  test('POST /api/qa/packages should validate URL format', async () => {
    const response = await api.create({
      specUrl: 'not-a-valid-url',
    });

    expect(response.status).toBe(400);
  });

  test('GET /api/qa/packages/:id should return package by ID', async () => {
    // First create a package
    const createData = testData.createPackage();
    const createResponse = await api.create(createData);
    expect(createResponse.status).toBe(201);
    createdPackageIds.push(createResponse.body.id);

    // Then fetch it
    const response = await api.getById(createResponse.body.id);

    expect(response.status).toBe(200);
    expect(response.body.id).toBe(createResponse.body.id);
    expect(response.body.specUrl).toBe(createData.specUrl);
  });

  test('GET /api/qa/packages/:id should return 404 for non-existent', async () => {
    const response = await api.getById('non-existent-id');

    expect(response.status).toBe(404);
  });

  test('PUT /api/qa/packages/:id should update package', async () => {
    // First create a package
    const createData = testData.createPackage();
    const createResponse = await api.create(createData);
    expect(createResponse.status).toBe(201);
    createdPackageIds.push(createResponse.body.id);

    // Update it
    const newDescription = 'Updated description';
    const updateResponse = await api.update(createResponse.body.id, {
      description: newDescription,
    });

    expect(updateResponse.status).toBe(200);
    expect(updateResponse.body.description).toBe(newDescription);
    expect(updateResponse.body.specUrl).toBe(createData.specUrl);
  });

  test('DELETE /api/qa/packages/:id should delete package', async () => {
    // First create a package
    const createData = testData.createPackage();
    const createResponse = await api.create(createData);
    expect(createResponse.status).toBe(201);

    // Delete it
    const deleteResponse = await api.deletePackage(createResponse.body.id);
    expect(deleteResponse.status).toBe(204);

    // Verify it's gone
    const getResponse = await api.getById(createResponse.body.id);
    expect(getResponse.status).toBe(404);
  });

  test('DELETE /api/qa/packages/:id should return 404 for non-existent', async () => {
    const response = await api.deletePackage('non-existent-id');

    expect(response.status).toBe(404);
  });

  test('POST /api/qa/packages/:id/runs should trigger test run', async () => {
    // First create a package
    const createData = testData.createPackage();
    const createResponse = await api.create(createData);
    expect(createResponse.status).toBe(201);
    createdPackageIds.push(createResponse.body.id);

    // Trigger run
    const runResponse = await api.triggerRun(createResponse.body.id);

    expect(runResponse.status).toBe(202);
    expect(runResponse.body).toHaveProperty('runId');
  });
});
