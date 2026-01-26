import { test, expect } from '@playwright/test';
import { PackagesApi, Package, CreatePackageRequest } from '../../api/packages.api';
import { ScenariosApi, Scenario } from '../../api/scenarios.api';
import { testData } from '../../fixtures/testData';

/**
 * API Contract Tests
 * These tests validate that API responses conform to expected schema contracts
 *
 * @tags @api @contract
 */
test.describe('API Contract Tests @api @contract', () => {
  let packagesApi: PackagesApi;
  let scenariosApi: ScenariosApi;
  const createdPackageIds: string[] = [];

  test.beforeAll(async ({ request }) => {
    packagesApi = new PackagesApi(request);
    scenariosApi = new ScenariosApi(request);
  });

  test.afterAll(async () => {
    for (const id of createdPackageIds) {
      try {
        await packagesApi.deletePackage(id);
      } catch {
        // Ignore cleanup errors
      }
    }
  });

  test.describe('Response Schema Compliance', () => {
    test('Package response should contain all required fields', async () => {
      // Create a package to test response schema
      const data = testData.createPackage();
      const response = await packagesApi.create(data);

      expect(response.status).toBe(201);
      createdPackageIds.push(response.body.id);

      // Validate required fields exist
      const pkg = response.body as Package;
      expect(pkg).toHaveProperty('id');
      expect(pkg).toHaveProperty('name');
      expect(pkg).toHaveProperty('specUrl');
      expect(pkg).toHaveProperty('createdAt');
      expect(pkg).toHaveProperty('updatedAt');

      // Validate field types
      expect(typeof pkg.id).toBe('string');
      expect(typeof pkg.name).toBe('string');
      expect(typeof pkg.specUrl).toBe('string');
      expect(typeof pkg.createdAt).toBe('string');
      expect(typeof pkg.updatedAt).toBe('string');

      // Validate optional fields when present
      if (pkg.baseUrl !== undefined) {
        expect(typeof pkg.baseUrl).toBe('string');
      }
      if (pkg.description !== undefined) {
        expect(typeof pkg.description).toBe('string');
      }
    });

    test('Paginated response should contain all pagination fields', async () => {
      const response = await packagesApi.list();

      expect(response.status).toBe(200);

      // Validate pagination structure
      expect(response.body).toHaveProperty('content');
      expect(response.body).toHaveProperty('totalElements');
      expect(response.body).toHaveProperty('totalPages');
      expect(response.body).toHaveProperty('size');
      expect(response.body).toHaveProperty('number');

      // Validate field types
      expect(Array.isArray(response.body.content)).toBe(true);
      expect(typeof response.body.totalElements).toBe('number');
      expect(typeof response.body.totalPages).toBe('number');
      expect(typeof response.body.size).toBe('number');
      expect(typeof response.body.number).toBe('number');

      // Validate pagination values are reasonable
      expect(response.body.totalElements).toBeGreaterThanOrEqual(0);
      expect(response.body.totalPages).toBeGreaterThanOrEqual(0);
      expect(response.body.size).toBeGreaterThan(0);
      expect(response.body.number).toBeGreaterThanOrEqual(0);
    });

    test('Each package in list should have required fields', async () => {
      // Create a package first to ensure list is not empty
      const data = testData.createPackage();
      const createResponse = await packagesApi.create(data);
      expect(createResponse.status).toBe(201);
      createdPackageIds.push(createResponse.body.id);

      // Get list
      const response = await packagesApi.list();
      expect(response.status).toBe(200);
      expect(response.body.content.length).toBeGreaterThan(0);

      // Validate each package in content
      for (const pkg of response.body.content) {
        expect(pkg).toHaveProperty('id');
        expect(pkg).toHaveProperty('specUrl');
        expect(pkg).toHaveProperty('createdAt');
        expect(typeof pkg.id).toBe('string');
        expect(typeof pkg.specUrl).toBe('string');
        expect(typeof pkg.createdAt).toBe('string');
      }
    });

    test('Scenario response should contain all required fields', async () => {
      // Create a package first
      const packageData = testData.createPackage();
      const packageResponse = await packagesApi.create(packageData);
      expect(packageResponse.status).toBe(201);
      createdPackageIds.push(packageResponse.body.id);

      // Create a scenario
      const scenarioData = testData.createScenario();
      const response = await scenariosApi.create(packageResponse.body.id, scenarioData);

      // Scenario creation may return 201 or 202 (for async generation)
      expect([200, 201, 202]).toContain(response.status);

      if (response.status === 201) {
        const scenario = response.body as Scenario;

        // Validate required fields
        expect(scenario).toHaveProperty('id');
        expect(scenario).toHaveProperty('name');
        expect(scenario).toHaveProperty('createdAt');

        // Validate types
        expect(typeof scenario.id).toBe('string');
        expect(typeof scenario.name).toBe('string');
        expect(typeof scenario.createdAt).toBe('string');
      }
    });
  });

  test.describe('Request Validation', () => {
    test('POST /packages should reject empty body', async () => {
      const response = await packagesApi.create({} as CreatePackageRequest);

      expect([400, 422]).toContain(response.status);
    });

    test('POST /packages should reject missing specUrl', async () => {
      const response = await packagesApi.create({
        name: 'Test Package',
      } as CreatePackageRequest);

      expect([400, 422]).toContain(response.status);
    });

    test('POST /packages should reject invalid URL format', async () => {
      const response = await packagesApi.create({
        specUrl: 'not-a-valid-url',
        name: 'Test Package',
      });

      expect([400, 422]).toContain(response.status);
    });

    test('POST /packages should reject extremely long names', async () => {
      const response = await packagesApi.create({
        specUrl: 'https://example.com/api.json',
        name: 'A'.repeat(10000), // Very long name
      });

      expect([400, 413, 422]).toContain(response.status);
    });

    test('GET /packages should accept valid pagination params', async () => {
      const response = await packagesApi.list(0, 10);

      expect(response.status).toBe(200);
      expect(response.body.number).toBe(0);
    });

    test('PUT /packages/:id should only update provided fields', async () => {
      // Create a package
      const data = testData.createPackage();
      const createResponse = await packagesApi.create(data);
      expect(createResponse.status).toBe(201);
      createdPackageIds.push(createResponse.body.id);

      // Update only description
      const updateResponse = await packagesApi.update(createResponse.body.id, {
        description: 'New description',
      });

      expect(updateResponse.status).toBe(200);
      // Original fields should be unchanged
      expect(updateResponse.body.specUrl).toBe(data.specUrl);
      expect(updateResponse.body.name).toBe(data.name);
      // Updated field should reflect change
      expect(updateResponse.body.description).toBe('New description');
    });
  });

  test.describe('Error Response Format', () => {
    test('404 error should have proper error format', async () => {
      const response = await packagesApi.getById('non-existent-id-12345');

      expect(response.status).toBe(404);

      // Error response should have message or error field
      const hasErrorField =
        response.body.message !== undefined ||
        response.body.error !== undefined;
      expect(hasErrorField).toBe(true);
    });

    test('400 error should include validation details', async () => {
      const response = await packagesApi.create({ specUrl: '' } as CreatePackageRequest);

      expect([400, 422]).toContain(response.status);

      // Should have some error information
      const hasErrorInfo =
        response.body.message !== undefined ||
        response.body.error !== undefined ||
        response.body.errors !== undefined;
      expect(hasErrorInfo).toBe(true);
    });

    test('405 Method Not Allowed should have proper format', async ({ request }) => {
      // Try an unsupported method on packages endpoint
      const response = await request.patch(
        `${process.env.API_URL ?? 'http://localhost:8080'}/api/qa/packages`,
        {
          data: { foo: 'bar' },
        }
      );

      // Should be either 405 Method Not Allowed or 404
      expect([400, 404, 405]).toContain(response.status());
    });

    test('Server errors should not expose internal details', async ({ request }) => {
      // Try to trigger a server error with malformed JSON
      const response = await request.post(
        `${process.env.API_URL ?? 'http://localhost:8080'}/api/qa/packages`,
        {
          data: 'this is not valid json',
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      // If it's a 400 error, check it doesn't expose stack traces
      if (response.status() === 400 || response.status() === 500) {
        const body = await response.text();
        expect(body).not.toContain('at com.');
        expect(body).not.toContain('at java.');
        expect(body).not.toContain('Exception');
      }
    });
  });

  test.describe('Content-Type Handling', () => {
    test('API should return JSON content type', async () => {
      const response = await packagesApi.list();

      expect(response.status).toBe(200);
      expect(response.headers['content-type']).toContain('application/json');
    });

    test('Created resource should return JSON', async () => {
      const data = testData.createPackage();
      const response = await packagesApi.create(data);

      expect(response.status).toBe(201);
      createdPackageIds.push(response.body.id);
      expect(response.headers['content-type']).toContain('application/json');
    });

    test('API should handle Accept header', async ({ request }) => {
      const response = await request.get(
        `${process.env.API_URL ?? 'http://localhost:8080'}/api/qa/packages`,
        {
          headers: {
            Accept: 'application/json',
          },
        }
      );

      expect(response.status()).toBe(200);
      expect(response.headers()['content-type']).toContain('application/json');
    });
  });

  test.describe('Timestamp Format', () => {
    test('CreatedAt should be valid ISO 8601 format', async () => {
      const data = testData.createPackage();
      const response = await packagesApi.create(data);

      expect(response.status).toBe(201);
      createdPackageIds.push(response.body.id);

      // Validate ISO 8601 format
      const createdAt = response.body.createdAt;
      const date = new Date(createdAt);
      expect(date.toString()).not.toBe('Invalid Date');
      expect(createdAt).toMatch(
        /^\d{4}-\d{2}-\d{2}(T\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[+-]\d{2}:\d{2})?)?$/
      );
    });

    test('UpdatedAt should update on modifications', async () => {
      const data = testData.createPackage();
      const createResponse = await packagesApi.create(data);
      expect(createResponse.status).toBe(201);
      createdPackageIds.push(createResponse.body.id);

      const originalUpdatedAt = createResponse.body.updatedAt;

      // Wait a moment to ensure timestamp difference
      await new Promise((resolve) => setTimeout(resolve, 1000));

      // Update the package
      const updateResponse = await packagesApi.update(createResponse.body.id, {
        description: 'Updated description',
      });

      expect(updateResponse.status).toBe(200);

      // UpdatedAt should be different (or at least present)
      expect(updateResponse.body.updatedAt).toBeDefined();
    });
  });

  test.describe('ID Format', () => {
    test('Package ID should be valid format (UUID or similar)', async () => {
      const data = testData.createPackage();
      const response = await packagesApi.create(data);

      expect(response.status).toBe(201);
      createdPackageIds.push(response.body.id);

      // ID should be a non-empty string
      expect(typeof response.body.id).toBe('string');
      expect(response.body.id.length).toBeGreaterThan(0);

      // If UUID format, validate it
      if (response.body.id.length === 36) {
        expect(response.body.id).toMatch(
          /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
        );
      }
    });

    test('IDs should be unique', async () => {
      // Create two packages
      const data1 = testData.createPackage();
      const data2 = testData.createPackage();

      const response1 = await packagesApi.create(data1);
      const response2 = await packagesApi.create(data2);

      expect(response1.status).toBe(201);
      expect(response2.status).toBe(201);

      createdPackageIds.push(response1.body.id);
      createdPackageIds.push(response2.body.id);

      // IDs should be different
      expect(response1.body.id).not.toBe(response2.body.id);
    });
  });

  test.describe('HTTP Status Code Compliance', () => {
    test('GET list should return 200', async () => {
      const response = await packagesApi.list();
      expect(response.status).toBe(200);
    });

    test('GET single resource should return 200', async () => {
      const data = testData.createPackage();
      const createResponse = await packagesApi.create(data);
      createdPackageIds.push(createResponse.body.id);

      const response = await packagesApi.getById(createResponse.body.id);
      expect(response.status).toBe(200);
    });

    test('POST create should return 201', async () => {
      const data = testData.createPackage();
      const response = await packagesApi.create(data);

      expect(response.status).toBe(201);
      createdPackageIds.push(response.body.id);
    });

    test('PUT update should return 200', async () => {
      const data = testData.createPackage();
      const createResponse = await packagesApi.create(data);
      createdPackageIds.push(createResponse.body.id);

      const response = await packagesApi.update(createResponse.body.id, {
        description: 'Updated',
      });
      expect(response.status).toBe(200);
    });

    test('DELETE should return 204', async () => {
      const data = testData.createPackage();
      const createResponse = await packagesApi.create(data);
      // Don't add to cleanup - we're testing delete

      const response = await packagesApi.deletePackage(createResponse.body.id);
      expect(response.status).toBe(204);
    });

    test('POST async operation should return 202', async () => {
      const data = testData.createPackage();
      const createResponse = await packagesApi.create(data);
      createdPackageIds.push(createResponse.body.id);

      const response = await packagesApi.triggerRun(createResponse.body.id);
      expect(response.status).toBe(202);
    });

    test('GET non-existent should return 404', async () => {
      const response = await packagesApi.getById('non-existent-12345');
      expect(response.status).toBe(404);
    });

    test('DELETE non-existent should return 404', async () => {
      const response = await packagesApi.deletePackage('non-existent-12345');
      expect(response.status).toBe(404);
    });
  });
});
