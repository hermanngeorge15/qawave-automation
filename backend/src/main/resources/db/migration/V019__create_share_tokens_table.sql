-- V019__create_share_tokens_table.sql
-- Description: Create share_tokens table for shareable run links
-- Author: Backend Agent
-- Date: 2026-01-31
-- Issue: #314 - Implement shareable run links with unique tokens

-- ============================================================================
-- PART 1: Create share_tokens table
-- ============================================================================

CREATE TABLE share_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL REFERENCES test_runs(id) ON DELETE CASCADE,
    token VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    view_count INTEGER NOT NULL DEFAULT 0,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT share_tokens_token_unique UNIQUE (token)
);

COMMENT ON TABLE share_tokens IS
'Stores tokens for sharing test run results via public URLs without authentication';

COMMENT ON COLUMN share_tokens.id IS 'Primary key UUID for the share token';
COMMENT ON COLUMN share_tokens.run_id IS 'Reference to the test run being shared';
COMMENT ON COLUMN share_tokens.token IS 'Unique secure token used in the shareable URL';
COMMENT ON COLUMN share_tokens.expires_at IS 'When this share token expires (default 7 days from creation)';
COMMENT ON COLUMN share_tokens.view_count IS 'Number of times this shared link has been accessed';
COMMENT ON COLUMN share_tokens.created_by IS 'Keycloak subject ID of user who created the share';
COMMENT ON COLUMN share_tokens.created_at IS 'When this share token was created';
COMMENT ON COLUMN share_tokens.revoked_at IS 'When this share token was revoked (null if still active)';

-- ============================================================================
-- PART 2: Create indexes for efficient queries
-- ============================================================================

-- Index for looking up share by token (primary access pattern)
CREATE INDEX idx_share_tokens_token ON share_tokens(token);

-- Index for finding all shares for a run
CREATE INDEX idx_share_tokens_run_id ON share_tokens(run_id);

-- Index for finding shares by creator
CREATE INDEX idx_share_tokens_created_by ON share_tokens(created_by);

-- Partial index for active (non-expired, non-revoked) tokens
CREATE INDEX idx_share_tokens_active ON share_tokens(token)
WHERE revoked_at IS NULL AND expires_at > NOW();

-- ============================================================================
-- PART 3: Add documentation
-- ============================================================================

COMMENT ON INDEX idx_share_tokens_token IS 'Primary lookup index for share token validation';
COMMENT ON INDEX idx_share_tokens_run_id IS 'Index for finding all share tokens for a test run';
COMMENT ON INDEX idx_share_tokens_created_by IS 'Index for finding shares created by a user';
COMMENT ON INDEX idx_share_tokens_active IS 'Partial index for quick active token validation';
