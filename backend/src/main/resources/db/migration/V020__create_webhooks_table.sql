-- V020__create_webhooks_table.sql
-- Description: Create tables for webhook configuration and delivery tracking
-- Author: Backend Agent
-- Date: 2026-01-31
-- Issue: #315 - Implement webhook notifications for run completion

-- ============================================================================
-- PART 1: Create webhook_configs table
-- ============================================================================

CREATE TABLE webhook_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    webhook_type VARCHAR(50) NOT NULL,
    events TEXT NOT NULL,
    headers_json TEXT,
    secret VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT webhook_configs_type_check CHECK (webhook_type IN ('SLACK', 'GENERIC', 'EMAIL'))
);

COMMENT ON TABLE webhook_configs IS
'Stores webhook configuration for notifications on test run events';

COMMENT ON COLUMN webhook_configs.id IS 'Primary key UUID';
COMMENT ON COLUMN webhook_configs.name IS 'User-friendly name for the webhook';
COMMENT ON COLUMN webhook_configs.url IS 'Webhook URL to POST to';
COMMENT ON COLUMN webhook_configs.webhook_type IS 'Type of webhook: SLACK, GENERIC, EMAIL';
COMMENT ON COLUMN webhook_configs.events IS 'Comma-separated list of events to trigger on';
COMMENT ON COLUMN webhook_configs.headers_json IS 'Custom headers as JSON object';
COMMENT ON COLUMN webhook_configs.secret IS 'Secret for signing webhook payloads (HMAC-SHA256)';
COMMENT ON COLUMN webhook_configs.is_active IS 'Whether webhook is active';
COMMENT ON COLUMN webhook_configs.created_by IS 'Keycloak subject ID of creator';

-- ============================================================================
-- PART 2: Create webhook_deliveries table
-- ============================================================================

CREATE TABLE webhook_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    webhook_config_id UUID NOT NULL REFERENCES webhook_configs(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    response_status INTEGER,
    response_body TEXT,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT webhook_deliveries_status_check CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'RETRYING'))
);

COMMENT ON TABLE webhook_deliveries IS
'Tracks individual webhook delivery attempts for debugging and retries';

COMMENT ON COLUMN webhook_deliveries.id IS 'Primary key UUID';
COMMENT ON COLUMN webhook_deliveries.webhook_config_id IS 'Reference to webhook configuration';
COMMENT ON COLUMN webhook_deliveries.event_type IS 'Event type that triggered this delivery';
COMMENT ON COLUMN webhook_deliveries.payload IS 'JSON payload sent to webhook';
COMMENT ON COLUMN webhook_deliveries.status IS 'Delivery status: PENDING, SUCCESS, FAILED, RETRYING';
COMMENT ON COLUMN webhook_deliveries.attempt_count IS 'Number of delivery attempts';
COMMENT ON COLUMN webhook_deliveries.last_attempt_at IS 'Time of last delivery attempt';
COMMENT ON COLUMN webhook_deliveries.next_retry_at IS 'Time for next retry attempt';
COMMENT ON COLUMN webhook_deliveries.response_status IS 'HTTP status code from webhook endpoint';
COMMENT ON COLUMN webhook_deliveries.response_body IS 'Response body from webhook endpoint (truncated)';
COMMENT ON COLUMN webhook_deliveries.error_message IS 'Error message if delivery failed';
COMMENT ON COLUMN webhook_deliveries.completed_at IS 'When delivery was completed (success or final failure)';

-- ============================================================================
-- PART 3: Create indexes
-- ============================================================================

-- Index for finding webhooks by creator
CREATE INDEX idx_webhook_configs_created_by ON webhook_configs(created_by);

-- Index for finding active webhooks
CREATE INDEX idx_webhook_configs_active ON webhook_configs(is_active) WHERE is_active = true;

-- Index for finding pending deliveries for retry
CREATE INDEX idx_webhook_deliveries_pending ON webhook_deliveries(status, next_retry_at)
WHERE status IN ('PENDING', 'RETRYING');

-- Index for finding deliveries by webhook
CREATE INDEX idx_webhook_deliveries_config_id ON webhook_deliveries(webhook_config_id);

-- Index for finding recent deliveries
CREATE INDEX idx_webhook_deliveries_created ON webhook_deliveries(created_at DESC);

-- ============================================================================
-- PART 4: Documentation
-- ============================================================================

COMMENT ON INDEX idx_webhook_configs_created_by IS 'Index for filtering webhooks by owner';
COMMENT ON INDEX idx_webhook_configs_active IS 'Partial index for active webhooks';
COMMENT ON INDEX idx_webhook_deliveries_pending IS 'Index for finding deliveries needing retry';
COMMENT ON INDEX idx_webhook_deliveries_config_id IS 'Index for finding deliveries by webhook config';
