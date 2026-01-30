# QAWave Load Testing Guide

This guide explains how to execute load tests and generate performance reports for QAWave.

## Prerequisites

1. **k6 installed**: https://k6.io/docs/getting-started/installation/
2. **Backend running**: Either local or staging environment
3. **Test data**: Database seeded with test packages

## Quick Start

```bash
cd e2e-tests/load-tests

# Run smoke test (baseline)
k6 run scenarios/smoke.js

# Run load test (normal traffic)
k6 run scenarios/load.js

# Run stress test (find breaking point)
k6 run scenarios/stress.js

# Run soak test (memory leaks)
k6 run scenarios/soak.js
```

## Environment Configuration

```bash
# Local development
export API_URL=http://localhost:8080

# Staging
export API_URL=https://api.staging.qawave.io

# With authentication (if required)
export AUTH_TOKEN=your-jwt-token
```

## Test Scenarios

### 1. Smoke Test (Baseline)
**Purpose**: Establish baseline performance metrics

```bash
k6 run scenarios/smoke.js
```

**Parameters**:
- VUs: 1
- Duration: 1 minute
- Thresholds: P95 < 1s, Error < 5%

### 2. Load Test (Normal Traffic)
**Purpose**: Verify performance under expected load

```bash
k6 run scenarios/load.js
```

**Parameters**:
- VUs: Ramp to 50
- Duration: 7 minutes
- Thresholds: P95 < 500ms, P99 < 1s, Error < 1%

### 3. Stress Test (Breaking Point)
**Purpose**: Find system limits and failure modes

```bash
k6 run scenarios/stress.js
```

**Parameters**:
- VUs: Ramp to 200
- Duration: 15 minutes
- Thresholds: P95 < 2s, Error < 10%

### 4. Soak Test (Stability)
**Purpose**: Detect memory leaks and resource exhaustion

```bash
k6 run scenarios/soak.js
```

**Parameters**:
- VUs: 30 (sustained)
- Duration: 34 minutes
- Thresholds: P95 < 500ms, P99 < 1s, Error < 1%

## Generating Reports

### JSON Output

```bash
k6 run --out json=results/load-test-$(date +%Y%m%d).json scenarios/load.js
```

### HTML Dashboard (k6 Cloud)

```bash
# Requires k6 cloud account
K6_CLOUD_TOKEN=your-token k6 run --out cloud scenarios/load.js
```

### InfluxDB + Grafana

```bash
# Start InfluxDB and Grafana
docker-compose -f docker-compose.monitoring.yml up -d

# Run tests with InfluxDB output
k6 run --out influxdb=http://localhost:8086/k6 scenarios/load.js
```

## Performance Report Template

After running tests, create a report using this template:

### Executive Summary

| Metric | Target | Actual | Status |
|--------|--------|--------|--------|
| P95 Latency | < 500ms | ? ms | ✅/❌ |
| P99 Latency | < 1000ms | ? ms | ✅/❌ |
| Error Rate | < 1% | ?% | ✅/❌ |
| Max VUs Sustained | 50 | ? | ✅/❌ |

### Baseline (Smoke Test)

```
Test Duration: 1 minute
Virtual Users: 1

Response Times:
- Min: ? ms
- Avg: ? ms
- P95: ? ms
- P99: ? ms
- Max: ? ms

Throughput:
- Requests/sec: ?
- Data received: ? MB

Errors:
- Total: ?
- Rate: ?%
```

### Load Test Results

```
Test Duration: 7 minutes
Peak Virtual Users: 50

Response Times:
- Min: ? ms
- Avg: ? ms
- P95: ? ms
- P99: ? ms
- Max: ? ms

Throughput:
- Requests/sec: ?
- Data received: ? MB

Errors:
- Total: ?
- Rate: ?%

Observations:
- [List any observations about performance degradation]
```

### Stress Test Results

```
Test Duration: 15 minutes
Peak Virtual Users: 200

Breaking Point:
- VUs at first failure: ?
- VUs at 5% error rate: ?
- VUs at 10% error rate: ?

Response Times at Breaking Point:
- P95: ? ms
- P99: ? ms

Recovery:
- Time to recover after load decrease: ? seconds

Failure Modes:
- [List observed failure modes]
```

### Soak Test Results

```
Test Duration: 34 minutes
Sustained Virtual Users: 30

Memory Trend:
- Start: ? MB
- End: ? MB
- Growth: ? MB/hour

Response Time Trend:
- Start P95: ? ms
- End P95: ? ms
- Degradation: ?%

Observations:
- [List any memory leaks or resource exhaustion]
```

### Bottleneck Analysis

| Component | Issue | Evidence | Recommendation |
|-----------|-------|----------|----------------|
| Database | ? | ? | ? |
| API | ? | ? | ? |
| Network | ? | ? | ? |
| Memory | ? | ? | ? |

### Recommendations

1. **Database**
   - [ ] Add index on X column
   - [ ] Optimize Y query

2. **Caching**
   - [ ] Cache Z endpoint
   - [ ] Increase TTL for W

3. **Code**
   - [ ] Optimize A function
   - [ ] Add connection pooling for B

4. **Infrastructure**
   - [ ] Scale to N replicas under load
   - [ ] Increase memory to X GB

## Automated Reporting

### CI Integration

Add to GitHub Actions:

```yaml
- name: Run Load Tests
  run: |
    cd e2e-tests/load-tests
    k6 run --out json=results/load.json scenarios/smoke.js

- name: Upload Results
  uses: actions/upload-artifact@v4
  with:
    name: load-test-results
    path: e2e-tests/load-tests/results/
```

### Scheduled Tests

Run load tests weekly:

```yaml
on:
  schedule:
    - cron: '0 3 * * 1'  # Monday 3 AM UTC
```

## Troubleshooting

### High Error Rates

1. Check API logs for errors
2. Verify database connections
3. Check for rate limiting
4. Review network connectivity

### Slow Response Times

1. Enable profiling on backend
2. Check database query times
3. Review N+1 query patterns
4. Check for blocking operations

### Memory Issues in k6

1. Reduce VU count
2. Simplify test script
3. Use streaming for large responses

---

*Last Updated: 2026-01-30*
*QA Agent*
