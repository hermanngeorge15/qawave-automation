# QAWave Load Tests

Performance and load tests for the QAWave API using [k6](https://k6.io/).

## Prerequisites

- [k6](https://k6.io/docs/getting-started/installation/) installed
- QAWave backend running (local or staging)

## Test Scenarios

| Scenario | VUs | Duration | Purpose |
|----------|-----|----------|---------|
| **Smoke** | 1 | 1 min | Baseline sanity check |
| **Load** | 50 | 7 min | Normal traffic simulation |
| **Stress** | 200 | 15 min | Find breaking point |
| **Soak** | 30 | 34 min | Stability over time |

## Quick Start

```bash
# Run smoke test (quick sanity check)
k6 run scenarios/smoke.js

# Run load test
k6 run scenarios/load.js

# Run stress test
k6 run scenarios/stress.js

# Run soak test (long duration)
k6 run scenarios/soak.js
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `API_URL` | `http://localhost:8080` | Backend API URL |
| `AUTH_TOKEN` | - | Bearer token for authenticated requests |
| `ENVIRONMENT` | `local` | Environment identifier for results |

### Running Against Different Environments

```bash
# Local development
k6 run scenarios/load.js

# Staging
API_URL=https://api.staging.qawave.io k6 run scenarios/load.js

# With authentication
AUTH_TOKEN=your-jwt-token k6 run scenarios/load.js
```

## Thresholds

### Smoke Test
- P95 response time < 1000ms
- Error rate < 5%

### Load Test
- P95 response time < 500ms
- P99 response time < 1000ms
- Error rate < 1%

### Stress Test
- P95 response time < 2000ms
- Error rate < 10%

### Soak Test
- P95 response time < 500ms
- P99 response time < 1000ms
- Error rate < 1%

## Results

Test results are saved to the `results/` directory:

- `smoke-summary.json` - Smoke test results
- `load-summary.json` - Load test results
- `stress-summary.json` - Stress test results
- `soak-summary.json` - Soak test results

### Viewing Results

```bash
# Run with web dashboard (k6 Cloud)
k6 run --out cloud scenarios/load.js

# Run with InfluxDB output
k6 run --out influxdb=http://localhost:8086/k6 scenarios/load.js

# Run with JSON output
k6 run --out json=results/output.json scenarios/load.js
```

## CI Integration

Add to GitHub Actions workflow:

```yaml
- name: Run k6 load tests
  uses: grafana/k6-action@v0.3.1
  with:
    filename: e2e-tests/load-tests/scenarios/smoke.js
  env:
    API_URL: ${{ vars.STAGING_API_URL }}
```

## Directory Structure

```
load-tests/
├── scenarios/
│   ├── smoke.js      # Quick sanity check
│   ├── load.js       # Normal traffic
│   ├── stress.js     # Breaking point
│   └── soak.js       # Long duration stability
├── utils/
│   └── helpers.js    # Shared utilities
├── results/          # Test output (gitignored)
└── README.md
```

## Interpreting Results

### Key Metrics

- **http_req_duration** - Request latency (lower is better)
- **http_reqs** - Requests per second (throughput)
- **error_rate** - Percentage of failed requests
- **vus** - Active virtual users

### Performance Analysis

1. **Smoke Test**: Verify basic functionality works
2. **Load Test**: Check P95/P99 meet SLAs under normal load
3. **Stress Test**: Find where system starts degrading
4. **Soak Test**: Check for memory leaks, connection issues

## Troubleshooting

### Connection Errors

```bash
# Check if API is reachable
curl -v $API_URL/actuator/health
```

### Rate Limiting

If seeing 429 errors, the API may have rate limiting. Adjust VU ramp-up or add delays.

### Memory Issues

If k6 runs out of memory, reduce the number of VUs or test duration.

## References

- [k6 Documentation](https://k6.io/docs/)
- [k6 Thresholds](https://k6.io/docs/using-k6/thresholds/)
- [k6 Metrics](https://k6.io/docs/using-k6/metrics/)
