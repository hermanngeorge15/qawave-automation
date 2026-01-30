/**
 * Stress Test
 *
 * Purpose: Find the system's breaking point under extreme load
 * Use case: Capacity planning, identify bottlenecks
 *
 * Load Profile:
 * - Ramp up gradually to 200 VUs
 * - Find breaking point
 * - Ramp down
 */
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { getBaseUrl, getHeaders, checkResponse } from '../utils/helpers.js';

// Custom metrics
const errorRate = new Rate('error_rate');
const requestDuration = new Trend('request_duration', true);
const failedRequests = new Counter('failed_requests');

// Test configuration
export const options = {
  stages: [
    { duration: '2m', target: 50 },    // Warm up
    { duration: '3m', target: 100 },   // Ramp to 100 VUs
    { duration: '3m', target: 150 },   // Push to 150 VUs
    { duration: '3m', target: 200 },   // Peak at 200 VUs
    { duration: '2m', target: 200 },   // Sustain peak
    { duration: '2m', target: 0 },     // Ramp down
  ],
  thresholds: {
    // More lenient thresholds for stress test - we expect some failures
    http_req_duration: ['p(95)<2000'],  // P95 under 2s
    error_rate: ['rate<0.1'],            // Error rate under 10%
  },
  tags: {
    testType: 'stress',
  },
};

const BASE_URL = getBaseUrl();

export default function () {
  const headers = getHeaders();

  group('High Load Operations', function () {
    // Heavy read operation
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/qa/packages?page=0&size=50`, { headers });
    requestDuration.add(Date.now() - start);

    const ok = checkResponse(res, 'list packages');
    errorRate.add(!ok);

    if (!ok) {
      failedRequests.add(1);
    }

    check(res, {
      'response time under 3s': (r) => r.timings.duration < 3000,
      'not rate limited (429)': (r) => r.status !== 429,
      'not server error (5xx)': (r) => r.status < 500,
    });
  });

  // Very short sleep to maximize load
  sleep(0.3);
}

export function handleSummary(data) {
  const metrics = data.metrics;
  const peakVUs = data.state?.maxVUs || 200;

  return {
    'results/stress-summary.json': JSON.stringify(data, null, 2),
    stdout: `
╔══════════════════════════════════════════════════════════╗
║                 STRESS TEST SUMMARY                      ║
╠══════════════════════════════════════════════════════════╣
║ Peak VUs:           ${peakVUs}
║ Total Requests:     ${metrics.http_reqs?.values?.count || 0}
║ Failed Requests:    ${metrics.failed_requests?.values?.count || 0}
║ Error Rate:         ${((metrics.error_rate?.values?.rate || 0) * 100).toFixed(2)}%
║
║ Response Times:
║   Min:   ${(metrics.http_req_duration?.values?.min || 0).toFixed(2)}ms
║   Avg:   ${(metrics.http_req_duration?.values?.avg || 0).toFixed(2)}ms
║   P90:   ${(metrics.http_req_duration?.values?.['p(90)'] || 0).toFixed(2)}ms
║   P95:   ${(metrics.http_req_duration?.values?.['p(95)'] || 0).toFixed(2)}ms
║   P99:   ${(metrics.http_req_duration?.values?.['p(99)'] || 0).toFixed(2)}ms
║   Max:   ${(metrics.http_req_duration?.values?.max || 0).toFixed(2)}ms
║
║ Breaking Point Analysis:
║   Check P99 response time at different VU levels
║   Review error rate progression during ramp-up
╚══════════════════════════════════════════════════════════╝
`,
  };
}
