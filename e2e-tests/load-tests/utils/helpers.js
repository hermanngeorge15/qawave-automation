/**
 * k6 Load Test Utilities
 *
 * Shared helper functions for all load test scenarios
 */
import { check } from 'k6';

/**
 * Get the base URL for API requests
 * Uses environment variable or defaults to localhost
 */
export function getBaseUrl() {
  return __ENV.API_URL || 'http://localhost:8080';
}

/**
 * Get default HTTP headers
 */
export function getHeaders() {
  const headers = {
    Accept: 'application/json',
  };

  // Add auth token if available
  if (__ENV.AUTH_TOKEN) {
    headers['Authorization'] = `Bearer ${__ENV.AUTH_TOKEN}`;
  }

  return headers;
}

/**
 * Standard response check helper
 * Returns true if response is successful
 */
export function checkResponse(response, name) {
  return check(response, {
    [`${name}: status is 2xx`]: (r) => r.status >= 200 && r.status < 300,
    [`${name}: response time < 2s`]: (r) => r.timings.duration < 2000,
    [`${name}: body is not empty`]: (r) => r.body && r.body.length > 0,
  });
}

/**
 * Generate unique test data
 */
export function generatePackageData() {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substring(7);

  return {
    name: `K6 Test Package ${timestamp}-${random}`,
    specUrl: 'https://petstore3.swagger.io/api/v3/openapi.json',
    baseUrl: 'https://petstore3.swagger.io/api/v3',
    description: `Load test package created at ${new Date().toISOString()}`,
  };
}

/**
 * Generate scenario data
 */
export function generateScenarioData() {
  const timestamp = Date.now();

  return {
    name: `K6 Test Scenario ${timestamp}`,
    description: 'Load test scenario',
  };
}

/**
 * Calculate percentile from sorted array
 */
export function percentile(arr, p) {
  if (!arr || arr.length === 0) return 0;
  const sorted = [...arr].sort((a, b) => a - b);
  const idx = Math.ceil(sorted.length * p) - 1;
  return sorted[Math.max(0, idx)];
}

/**
 * Format bytes to human readable
 */
export function formatBytes(bytes) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

/**
 * Format duration in ms to human readable
 */
export function formatDuration(ms) {
  if (ms < 1000) return `${ms.toFixed(0)}ms`;
  if (ms < 60000) return `${(ms / 1000).toFixed(2)}s`;
  return `${(ms / 60000).toFixed(2)}m`;
}

/**
 * Sleep for random duration within range
 */
export function randomSleep(minMs, maxMs) {
  const duration = minMs + Math.random() * (maxMs - minMs);
  return __VU ? sleep(duration / 1000) : null;
}

/**
 * Get environment configuration summary
 */
export function getConfig() {
  return {
    baseUrl: getBaseUrl(),
    hasAuth: !!__ENV.AUTH_TOKEN,
    environment: __ENV.ENVIRONMENT || 'local',
  };
}
