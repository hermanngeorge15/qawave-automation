import { faker } from '@faker-js/faker';

/**
 * Test data factories for E2E tests
 */
export const testData = {
  /**
   * Generate test data for creating a QA package
   */
  createPackage: () => ({
    name: `Test Package ${faker.string.alphanumeric(8)}`,
    specUrl: 'https://petstore3.swagger.io/api/v3/openapi.json',
    baseUrl: 'https://petstore3.swagger.io/api/v3',
    description: faker.lorem.sentence(),
  }),

  /**
   * Generate test data for creating a scenario
   */
  createScenario: () => ({
    name: `Test Scenario ${faker.string.alphanumeric(8)}`,
    description: faker.lorem.paragraph(),
  }),

  /**
   * Generate unique email for test user
   */
  uniqueEmail: () =>
    `test-${Date.now()}-${faker.string.alphanumeric(4)}@example.com`,

  /**
   * Generate test user data
   */
  createUser: () => ({
    email: `test-${Date.now()}-${faker.string.alphanumeric(4)}@example.com`,
    password: faker.internet.password({ length: 12 }),
    name: faker.person.fullName(),
  }),

  /**
   * Generate random test run configuration
   */
  createTestRunConfig: () => ({
    environment: faker.helpers.arrayElement(['development', 'staging', 'production']),
    parallelism: faker.number.int({ min: 1, max: 4 }),
    retryCount: faker.number.int({ min: 0, max: 3 }),
  }),
};

export type PackageData = ReturnType<typeof testData.createPackage>;
export type ScenarioData = ReturnType<typeof testData.createScenario>;
export type UserData = ReturnType<typeof testData.createUser>;
export type TestRunConfig = ReturnType<typeof testData.createTestRunConfig>;
