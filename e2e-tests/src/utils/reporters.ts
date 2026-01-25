import { Reporter, TestCase, TestResult, FullResult } from '@playwright/test/reporter';

/**
 * Custom reporter utilities for QAWave E2E tests
 */

/**
 * Console summary reporter
 */
export class ConsoleSummaryReporter implements Reporter {
  private passed = 0;
  private failed = 0;
  private skipped = 0;
  private startTime: number = 0;

  onBegin(): void {
    this.startTime = Date.now();
    console.log('\n========================================');
    console.log('  QAWave E2E Test Suite');
    console.log('========================================\n');
  }

  onTestEnd(test: TestCase, result: TestResult): void {
    const status = result.status;
    const icon = status === 'passed' ? '✓' : status === 'failed' ? '✗' : '○';

    if (status === 'passed') {
      this.passed++;
      console.log(`  ${icon} ${test.title}`);
    } else if (status === 'failed') {
      this.failed++;
      console.log(`  ${icon} ${test.title}`);
      if (result.error) {
        console.log(`    Error: ${result.error.message}`);
      }
    } else {
      this.skipped++;
      console.log(`  ${icon} ${test.title} (skipped)`);
    }
  }

  onEnd(result: FullResult): void {
    const duration = ((Date.now() - this.startTime) / 1000).toFixed(2);
    const total = this.passed + this.failed + this.skipped;

    console.log('\n----------------------------------------');
    console.log(`  Results: ${this.passed}/${total} passed`);
    console.log(`  Duration: ${duration}s`);
    console.log(`  Status: ${result.status}`);
    console.log('----------------------------------------\n');
  }
}

/**
 * Format test duration in human readable format
 */
export function formatDuration(ms: number): string {
  if (ms < 1000) {
    return `${ms}ms`;
  } else if (ms < 60000) {
    return `${(ms / 1000).toFixed(2)}s`;
  } else {
    const minutes = Math.floor(ms / 60000);
    const seconds = ((ms % 60000) / 1000).toFixed(0);
    return `${minutes}m ${seconds}s`;
  }
}

/**
 * Generate test summary object
 */
export interface TestSummary {
  total: number;
  passed: number;
  failed: number;
  skipped: number;
  duration: number;
  passRate: number;
}

export function generateSummary(results: TestResult[]): TestSummary {
  const passed = results.filter((r) => r.status === 'passed').length;
  const failed = results.filter((r) => r.status === 'failed').length;
  const skipped = results.filter((r) => r.status === 'skipped').length;
  const total = results.length;
  const duration = results.reduce((acc, r) => acc + r.duration, 0);

  return {
    total,
    passed,
    failed,
    skipped,
    duration,
    passRate: total > 0 ? (passed / total) * 100 : 0,
  };
}
