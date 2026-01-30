/**
 * Smoke Load Test
 *
 * Purpose: Verify the system works under minimal load (baseline test)
 * Use case: Quick sanity check after deployments
 *
 * Load Profile:
 * - 1 virtual user
 * - 1 minute duration
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { getBaseUrl, getHeaders, checkResponse } from '../utils/helpers.js';

// Custom metrics
const errorRate = new Rate('error_rate');
const packageListDuration = new Trend('package_list_duration');
const packageCreateDuration = new Trend('package_create_duration');

// Test configuration
export const options = {
  vus: 1,
  duration: '1m',
  thresholds: {
    http_req_duration: ['p(95)<1000'],  // 95% of requests under 1s
    error_rate: ['rate<0.05'],           // Error rate under 5%
  },
  tags: {
    testType: 'smoke',
  },
};

const BASE_URL = getBaseUrl();

export default function () {
  const headers = getHeaders();

  // Test 1: List packages
  const listStart = Date.now();
  const listRes = http.get(`${BASE_URL}/api/qa/packages`, { headers });
  packageListDuration.add(Date.now() - listStart);

  const listOk = checkResponse(listRes, 'list packages');
  errorRate.add(!listOk);

  check(listRes, {
    'list: has content array': (r) => {
      const body = JSON.parse(r.body);
      return Array.isArray(body.content);
    },
  });

  sleep(1);

  // Test 2: Create and delete a package
  const packageData = {
    name: `Load Test Package ${Date.now()}`,
    specUrl: 'https://petstore3.swagger.io/api/v3/openapi.json',
    description: 'Created by k6 smoke test',
  };

  const createStart = Date.now();
  const createRes = http.post(
    `${BASE_URL}/api/qa/packages`,
    JSON.stringify(packageData),
    { headers: { ...headers, 'Content-Type': 'application/json' } }
  );
  packageCreateDuration.add(Date.now() - createStart);

  const createOk = check(createRes, {
    'create: status is 201': (r) => r.status === 201,
    'create: has id': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.id !== undefined;
      } catch {
        return false;
      }
    },
  });
  errorRate.add(!createOk);

  // Cleanup: Delete the created package
  if (createRes.status === 201) {
    try {
      const body = JSON.parse(createRes.body);
      if (body.id) {
        http.del(`${BASE_URL}/api/qa/packages/${body.id}`, { headers });
      }
    } catch {
      // Ignore cleanup errors
    }
  }

  sleep(1);
}

export function handleSummary(data) {
  return {
    'results/smoke-summary.json': JSON.stringify(data, null, 2),
  };
}
