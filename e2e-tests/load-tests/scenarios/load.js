/**
 * Load Test
 *
 * Purpose: Verify system performance under expected normal traffic
 * Use case: Regular performance validation, capacity planning
 *
 * Load Profile:
 * - Ramp up to 50 VUs over 1 minute
 * - Sustain 50 VUs for 5 minutes
 * - Ramp down over 1 minute
 */
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { getBaseUrl, getHeaders, checkResponse } from '../utils/helpers.js';

// Custom metrics
const errorRate = new Rate('error_rate');
const packageListDuration = new Trend('package_list_duration', true);
const packageGetDuration = new Trend('package_get_duration', true);
const packageCreateDuration = new Trend('package_create_duration', true);
const packagesCreated = new Counter('packages_created');
const packagesDeleted = new Counter('packages_deleted');

// Test configuration
export const options = {
  stages: [
    { duration: '1m', target: 50 },   // Ramp up to 50 VUs
    { duration: '5m', target: 50 },   // Sustain 50 VUs
    { duration: '1m', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],  // P95 < 500ms, P99 < 1s
    error_rate: ['rate<0.01'],                        // Error rate under 1%
    package_list_duration: ['p(95)<400'],
    package_get_duration: ['p(95)<300'],
    package_create_duration: ['p(95)<1000'],
  },
  tags: {
    testType: 'load',
  },
};

const BASE_URL = getBaseUrl();

// Shared data for test coordination
const createdPackageIds = [];

export default function () {
  const headers = getHeaders();
  const jsonHeaders = { ...headers, 'Content-Type': 'application/json' };

  group('Read Operations', function () {
    // List packages
    const listStart = Date.now();
    const listRes = http.get(`${BASE_URL}/api/qa/packages?page=0&size=20`, { headers });
    packageListDuration.add(Date.now() - listStart);

    const listOk = checkResponse(listRes, 'list packages');
    errorRate.add(!listOk);

    check(listRes, {
      'list: has pagination info': (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.totalElements !== undefined;
        } catch {
          return false;
        }
      },
    });

    // Get a random package if available
    if (listRes.status === 200) {
      try {
        const packages = JSON.parse(listRes.body).content;
        if (packages.length > 0) {
          const randomPkg = packages[Math.floor(Math.random() * packages.length)];

          const getStart = Date.now();
          const getRes = http.get(`${BASE_URL}/api/qa/packages/${randomPkg.id}`, { headers });
          packageGetDuration.add(Date.now() - getStart);

          const getOk = checkResponse(getRes, 'get package');
          errorRate.add(!getOk);
        }
      } catch {
        // Ignore parse errors
      }
    }
  });

  sleep(1);

  group('Write Operations', function () {
    // Create a package (10% of iterations)
    if (Math.random() < 0.1) {
      const packageData = {
        name: `Load Test ${Date.now()}-${Math.random().toString(36).substring(7)}`,
        specUrl: 'https://petstore3.swagger.io/api/v3/openapi.json',
        description: 'Created by k6 load test',
      };

      const createStart = Date.now();
      const createRes = http.post(
        `${BASE_URL}/api/qa/packages`,
        JSON.stringify(packageData),
        { headers: jsonHeaders }
      );
      packageCreateDuration.add(Date.now() - createStart);

      const createOk = check(createRes, {
        'create: status is 201': (r) => r.status === 201,
      });
      errorRate.add(!createOk);

      if (createRes.status === 201) {
        packagesCreated.add(1);
        try {
          const body = JSON.parse(createRes.body);
          if (body.id) {
            createdPackageIds.push(body.id);
          }
        } catch {
          // Ignore
        }
      }
    }
  });

  sleep(1);
}

// Cleanup phase
export function teardown() {
  const headers = getHeaders();

  for (const id of createdPackageIds.slice(0, 100)) {
    try {
      const delRes = http.del(`${BASE_URL}/api/qa/packages/${id}`, { headers });
      if (delRes.status === 204) {
        packagesDeleted.add(1);
      }
    } catch {
      // Ignore cleanup errors
    }
    sleep(0.1);
  }
}

export function handleSummary(data) {
  return {
    'results/load-summary.json': JSON.stringify(data, null, 2),
    stdout: generateTextSummary(data),
  };
}

function generateTextSummary(data) {
  const metrics = data.metrics;
  return `
╔══════════════════════════════════════════════════════════╗
║                  LOAD TEST SUMMARY                       ║
╠══════════════════════════════════════════════════════════╣
║ Total Requests:     ${metrics.http_reqs?.values?.count || 0}
║ Failed Requests:    ${metrics.http_req_failed?.values?.passes || 0}
║ Error Rate:         ${((metrics.error_rate?.values?.rate || 0) * 100).toFixed(2)}%
║
║ Response Times:
║   P50:  ${(metrics.http_req_duration?.values?.['p(50)'] || 0).toFixed(2)}ms
║   P95:  ${(metrics.http_req_duration?.values?.['p(95)'] || 0).toFixed(2)}ms
║   P99:  ${(metrics.http_req_duration?.values?.['p(99)'] || 0).toFixed(2)}ms
║
║ Packages:
║   Created: ${metrics.packages_created?.values?.count || 0}
║   Deleted: ${metrics.packages_deleted?.values?.count || 0}
╚══════════════════════════════════════════════════════════╝
`;
}
