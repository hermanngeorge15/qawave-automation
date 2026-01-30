/**
 * Soak Test
 *
 * Purpose: Verify system stability over extended period
 * Use case: Memory leak detection, resource exhaustion, connection pool issues
 *
 * Load Profile:
 * - 30 VUs sustained for 30 minutes
 */
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';
import { getBaseUrl, getHeaders, checkResponse } from '../utils/helpers.js';

// Custom metrics
const errorRate = new Rate('error_rate');
const requestDuration = new Trend('request_duration', true);
const activeConnections = new Gauge('active_connections');
const memoryUsage = new Gauge('memory_estimate');

// Test configuration
export const options = {
  stages: [
    { duration: '2m', target: 30 },   // Ramp up
    { duration: '30m', target: 30 },  // Soak at 30 VUs
    { duration: '2m', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    error_rate: ['rate<0.01'],
  },
  tags: {
    testType: 'soak',
  },
};

const BASE_URL = getBaseUrl();

// Track iteration for periodic reporting
let iteration = 0;

export default function () {
  iteration++;
  const headers = getHeaders();

  group('Sustained Operations', function () {
    // Mix of read and write operations
    const operations = [
      // 70% list operations
      ...Array(7).fill(() => {
        const res = http.get(`${BASE_URL}/api/qa/packages`, { headers });
        return checkResponse(res, 'list');
      }),
      // 20% get operations
      ...Array(2).fill(() => {
        const listRes = http.get(`${BASE_URL}/api/qa/packages?size=1`, { headers });
        if (listRes.status === 200) {
          try {
            const pkg = JSON.parse(listRes.body).content[0];
            if (pkg) {
              const res = http.get(`${BASE_URL}/api/qa/packages/${pkg.id}`, { headers });
              return checkResponse(res, 'get');
            }
          } catch {
            return true;
          }
        }
        return listRes.status === 200;
      }),
      // 10% health check
      () => {
        const res = http.get(`${BASE_URL}/actuator/health`, { headers });
        return res.status === 200;
      },
    ];

    const operation = operations[Math.floor(Math.random() * operations.length)];
    const start = Date.now();
    const success = operation();
    requestDuration.add(Date.now() - start);
    errorRate.add(!success);
  });

  // Track approximate memory/connections (synthetic)
  activeConnections.add(__VU);

  // Periodic status log (every 100 iterations per VU)
  if (iteration % 100 === 0) {
    console.log(`VU ${__VU}: Completed ${iteration} iterations`);
  }

  sleep(1);
}

export function handleSummary(data) {
  const metrics = data.metrics;
  const durationMinutes = Math.round(data.state?.testRunDurationMs / 60000) || 0;

  return {
    'results/soak-summary.json': JSON.stringify(data, null, 2),
    stdout: `
╔══════════════════════════════════════════════════════════╗
║                  SOAK TEST SUMMARY                       ║
╠══════════════════════════════════════════════════════════╣
║ Duration:           ${durationMinutes} minutes
║ Total Requests:     ${metrics.http_reqs?.values?.count || 0}
║ Error Rate:         ${((metrics.error_rate?.values?.rate || 0) * 100).toFixed(4)}%
║
║ Response Times (should remain stable over time):
║   Min:   ${(metrics.http_req_duration?.values?.min || 0).toFixed(2)}ms
║   Avg:   ${(metrics.http_req_duration?.values?.avg || 0).toFixed(2)}ms
║   P95:   ${(metrics.http_req_duration?.values?.['p(95)'] || 0).toFixed(2)}ms
║   P99:   ${(metrics.http_req_duration?.values?.['p(99)'] || 0).toFixed(2)}ms
║   Max:   ${(metrics.http_req_duration?.values?.max || 0).toFixed(2)}ms
║
║ Stability Indicators:
║   Check if P95/P99 increased over test duration
║   Review error rate trend over time
║   Check system memory/CPU if available
╚══════════════════════════════════════════════════════════╝
`,
  };
}
