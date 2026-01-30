-- seed.sql
-- Sample data for local development
-- Author: Database Agent
-- Date: 2026-01-30
--
-- Usage: psql -U qawave -d qawave -f seed.sql
-- Or:    make db-seed
--
-- This script is IDEMPOTENT - safe to run multiple times
-- Uses ON CONFLICT DO UPDATE to handle existing data

-- ============================================================================
-- CLEANUP (optional - uncomment to reset)
-- ============================================================================
-- TRUNCATE qa_package_events, ai_logs, test_step_results, test_runs, test_scenarios, qa_packages CASCADE;

-- ============================================================================
-- SAMPLE QA PACKAGES
-- ============================================================================

-- Package 1: Pet Store API (Completed)
INSERT INTO qa_packages (
    id, name, description, base_url, spec_url, status, mode,
    scenarios_total, scenarios_passed, scenarios_failed, scenarios_skipped,
    user_id, triggered_by, created_by,
    started_at, finished_at, duration_ms,
    created_at, updated_at
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Pet Store API Tests',
    'Comprehensive test suite for the Swagger Pet Store API. Tests CRUD operations for pets, store orders, and user management.',
    'https://petstore3.swagger.io/api/v3',
    'https://petstore3.swagger.io/api/v3/openapi.json',
    'COMPLETE',
    'STANDARD',
    12, 10, 2, 0,
    'user-001', 'tester@qawave.local', 'tester@qawave.local',
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '1 hour 45 minutes',
    900000,
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '1 hour 45 minutes'
) ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    updated_at = NOW();

-- Package 2: Todo API (Running)
INSERT INTO qa_packages (
    id, name, description, base_url, spec_url, status, mode,
    scenarios_total, scenarios_passed, scenarios_failed, scenarios_skipped,
    user_id, triggered_by, created_by,
    started_at,
    created_at, updated_at
) VALUES (
    '22222222-2222-2222-2222-222222222222',
    'Todo API Tests',
    'Test suite for a simple Todo REST API. Validates task creation, updates, and deletion.',
    'https://jsonplaceholder.typicode.com',
    NULL,
    'EXECUTING',
    'STANDARD',
    8, 5, 0, 0,
    'user-001', 'tester@qawave.local', 'tester@qawave.local',
    NOW() - INTERVAL '10 minutes',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '10 minutes'
) ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    updated_at = NOW();

-- Package 3: User Management API (Failed)
INSERT INTO qa_packages (
    id, name, description, base_url, spec_url, status, mode,
    scenarios_total, scenarios_passed, scenarios_failed, scenarios_skipped,
    user_id, triggered_by, created_by,
    started_at, finished_at, duration_ms,
    error_message,
    created_at, updated_at
) VALUES (
    '33333333-3333-3333-3333-333333333333',
    'User Management API Tests',
    'Test suite for user authentication and management endpoints.',
    'https://reqres.in/api',
    'https://reqres.in/api/openapi.json',
    'FAILED_EXECUTION',
    'STANDARD',
    6, 3, 3, 0,
    'user-002', 'admin@qawave.local', 'admin@qawave.local',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '23 hours 30 minutes',
    1800000,
    'Connection timeout on endpoint /users/create',
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '23 hours 30 minutes'
) ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    updated_at = NOW();

-- Package 4: Security Tests (Pending)
INSERT INTO qa_packages (
    id, name, description, base_url, spec_url, status, mode,
    user_id, triggered_by, created_by,
    created_at, updated_at
) VALUES (
    '44444444-4444-4444-4444-444444444444',
    'API Security Tests',
    'OWASP API Top 10 security tests for the main application API.',
    'https://api.qawave.local',
    NULL,
    'REQUESTED',
    'SECURITY',
    'user-002', 'admin@qawave.local', 'admin@qawave.local',
    NOW() - INTERVAL '30 minutes',
    NOW() - INTERVAL '30 minutes'
) ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    updated_at = NOW();

-- Package 5: Performance Tests (Generating)
INSERT INTO qa_packages (
    id, name, description, base_url, spec_url, status, mode,
    user_id, triggered_by, created_by,
    created_at, updated_at
) VALUES (
    '55555555-5555-5555-5555-555555555555',
    'API Performance Tests',
    'Load and stress testing scenarios for critical API endpoints.',
    'https://api.qawave.local',
    NULL,
    'GENERATING',
    'PERFORMANCE',
    'user-001', 'tester@qawave.local', 'tester@qawave.local',
    NOW() - INTERVAL '5 minutes',
    NOW() - INTERVAL '2 minutes'
) ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    updated_at = NOW();

-- ============================================================================
-- SAMPLE TEST SCENARIOS
-- ============================================================================

-- Scenarios for Pet Store API
INSERT INTO test_scenarios (
    id, qa_package_id, name, description, source, operation_id, status,
    steps_json, tags, priority, user_id,
    created_at, updated_at
) VALUES
(
    'aaaa1111-1111-1111-1111-111111111111',
    '11111111-1111-1111-1111-111111111111',
    'Create and retrieve a pet',
    'Test creating a new pet and verifying it can be retrieved',
    'AI_GENERATED',
    'addPet',
    'ACTIVE',
    '[
        {"name": "Create pet", "method": "POST", "endpoint": "/pet", "body": {"name": "Fluffy", "status": "available"}},
        {"name": "Get pet", "method": "GET", "endpoint": "/pet/${petId}", "expected": {"status": 200}}
    ]'::jsonb,
    ARRAY['crud', 'pets', 'happy-path'],
    5,
    'user-001',
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '3 days'
),
(
    'aaaa2222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'Update pet information',
    'Test updating an existing pet''s details',
    'AI_GENERATED',
    'updatePet',
    'ACTIVE',
    '[
        {"name": "Create pet", "method": "POST", "endpoint": "/pet", "body": {"name": "Max", "status": "available"}},
        {"name": "Update pet", "method": "PUT", "endpoint": "/pet", "body": {"name": "Max Updated", "status": "sold"}},
        {"name": "Verify update", "method": "GET", "endpoint": "/pet/${petId}", "expected": {"body.name": "Max Updated"}}
    ]'::jsonb,
    ARRAY['crud', 'pets', 'update'],
    4,
    'user-001',
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '3 days'
),
(
    'aaaa3333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    'Delete pet and verify removal',
    'Test deleting a pet and confirming it no longer exists',
    'AI_GENERATED',
    'deletePet',
    'ACTIVE',
    '[
        {"name": "Create pet", "method": "POST", "endpoint": "/pet", "body": {"name": "ToDelete", "status": "available"}},
        {"name": "Delete pet", "method": "DELETE", "endpoint": "/pet/${petId}", "expected": {"status": 200}},
        {"name": "Verify deleted", "method": "GET", "endpoint": "/pet/${petId}", "expected": {"status": 404}}
    ]'::jsonb,
    ARRAY['crud', 'pets', 'delete'],
    3,
    'user-001',
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '3 days'
),
(
    'aaaa4444-4444-4444-4444-444444444444',
    '11111111-1111-1111-1111-111111111111',
    'Find pets by status',
    'Test finding pets with different status values',
    'AI_GENERATED',
    'findPetsByStatus',
    'ACTIVE',
    '[
        {"name": "Find available pets", "method": "GET", "endpoint": "/pet/findByStatus?status=available", "expected": {"status": 200}},
        {"name": "Find pending pets", "method": "GET", "endpoint": "/pet/findByStatus?status=pending", "expected": {"status": 200}},
        {"name": "Find sold pets", "method": "GET", "endpoint": "/pet/findByStatus?status=sold", "expected": {"status": 200}}
    ]'::jsonb,
    ARRAY['query', 'pets', 'filter'],
    4,
    'user-001',
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '3 days'
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    updated_at = NOW();

-- Scenarios for Todo API
INSERT INTO test_scenarios (
    id, qa_package_id, name, description, source, status,
    steps_json, tags, priority, user_id,
    created_at, updated_at
) VALUES
(
    'bbbb1111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    'List all todos',
    'Retrieve and verify the list of todos',
    'AI_GENERATED',
    'ACTIVE',
    '[
        {"name": "Get all todos", "method": "GET", "endpoint": "/todos", "expected": {"status": 200}},
        {"name": "Verify array response", "method": "GET", "endpoint": "/todos", "assertions": [{"type": "isArray", "path": "$"}]}
    ]'::jsonb,
    ARRAY['list', 'todos'],
    5,
    'user-001',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'
),
(
    'bbbb2222-2222-2222-2222-222222222222',
    '22222222-2222-2222-2222-222222222222',
    'Create a new todo',
    'Test creating a new todo item',
    'AI_GENERATED',
    'ACTIVE',
    '[
        {"name": "Create todo", "method": "POST", "endpoint": "/todos", "body": {"title": "Test todo", "completed": false}, "expected": {"status": 201}}
    ]'::jsonb,
    ARRAY['crud', 'todos', 'create'],
    5,
    'user-001',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    updated_at = NOW();

-- ============================================================================
-- SAMPLE TEST RUNS
-- ============================================================================

-- Successful run for Pet Store scenario 1
INSERT INTO test_runs (
    id, scenario_id, qa_package_id, base_url, status,
    steps_total, steps_passed, steps_failed, steps_skipped,
    duration_ms, user_id, triggered_by, trigger_source,
    started_at, finished_at, created_at
) VALUES
(
    'cccc1111-1111-1111-1111-111111111111',
    'aaaa1111-1111-1111-1111-111111111111',
    '11111111-1111-1111-1111-111111111111',
    'https://petstore3.swagger.io/api/v3',
    'PASSED',
    2, 2, 0, 0,
    1250,
    'user-001', 'tester@qawave.local', 'MANUAL',
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '1 hour 59 minutes',
    NOW() - INTERVAL '2 hours'
),
-- Failed run for Pet Store scenario 3
(
    'cccc2222-2222-2222-2222-222222222222',
    'aaaa3333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    'https://petstore3.swagger.io/api/v3',
    'FAILED',
    3, 2, 1, 0,
    2100,
    'user-001', 'tester@qawave.local', 'MANUAL',
    NOW() - INTERVAL '1 hour 50 minutes',
    NOW() - INTERVAL '1 hour 49 minutes',
    NOW() - INTERVAL '1 hour 50 minutes'
),
-- Running run for Todo API
(
    'cccc3333-3333-3333-3333-333333333333',
    'bbbb1111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    'https://jsonplaceholder.typicode.com',
    'RUNNING',
    2, 1, 0, 0,
    NULL,
    'user-001', 'tester@qawave.local', 'API',
    NOW() - INTERVAL '5 minutes',
    NULL,
    NOW() - INTERVAL '5 minutes'
)
ON CONFLICT (id) DO UPDATE SET
    status = EXCLUDED.status,
    updated_at = NOW();

-- ============================================================================
-- SAMPLE TEST STEP RESULTS
-- ============================================================================

-- Step results for successful run
INSERT INTO test_step_results (
    id, run_id, step_index, step_name, status,
    request_method, request_url, request_headers, request_body,
    actual_status, actual_headers, actual_body,
    expected_status, passed, duration_ms,
    started_at, finished_at, created_at
) VALUES
(
    'dddd1111-1111-1111-1111-111111111111',
    'cccc1111-1111-1111-1111-111111111111',
    0,
    'Create pet',
    'PASSED',
    'POST',
    'https://petstore3.swagger.io/api/v3/pet',
    '{"Content-Type": "application/json"}'::jsonb,
    '{"name": "Fluffy", "status": "available"}',
    200,
    '{"Content-Type": "application/json"}'::jsonb,
    '{"id": 12345, "name": "Fluffy", "status": "available"}',
    200,
    true,
    650,
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '1 hour 59 minutes 59 seconds',
    NOW() - INTERVAL '2 hours'
),
(
    'dddd2222-2222-2222-2222-222222222222',
    'cccc1111-1111-1111-1111-111111111111',
    1,
    'Get pet',
    'PASSED',
    'GET',
    'https://petstore3.swagger.io/api/v3/pet/12345',
    '{"Accept": "application/json"}'::jsonb,
    NULL,
    200,
    '{"Content-Type": "application/json"}'::jsonb,
    '{"id": 12345, "name": "Fluffy", "status": "available"}',
    200,
    true,
    450,
    NOW() - INTERVAL '1 hour 59 minutes 58 seconds',
    NOW() - INTERVAL '1 hour 59 minutes 57 seconds',
    NOW() - INTERVAL '1 hour 59 minutes 58 seconds'
),
-- Failed step
(
    'dddd3333-3333-3333-3333-333333333333',
    'cccc2222-2222-2222-2222-222222222222',
    2,
    'Verify deleted',
    'FAILED',
    'GET',
    'https://petstore3.swagger.io/api/v3/pet/99999',
    '{"Accept": "application/json"}'::jsonb,
    NULL,
    200,  -- Got 200 instead of expected 404
    '{"Content-Type": "application/json"}'::jsonb,
    '{"id": 99999, "name": "ToDelete", "status": "available"}',
    404,
    false,
    380,
    NOW() - INTERVAL '1 hour 49 minutes 30 seconds',
    NOW() - INTERVAL '1 hour 49 minutes 29 seconds',
    NOW() - INTERVAL '1 hour 49 minutes 30 seconds'
)
ON CONFLICT (id) DO UPDATE SET
    status = EXCLUDED.status;

-- ============================================================================
-- SAMPLE QA PACKAGE EVENTS
-- ============================================================================

INSERT INTO qa_package_events (
    id, qa_package_id, event_type, event_data, user_id, created_at
) VALUES
(
    'eeee1111-1111-1111-1111-111111111111',
    '11111111-1111-1111-1111-111111111111',
    'REQUESTED',
    '{"source": "web-ui"}'::jsonb,
    'user-001',
    NOW() - INTERVAL '3 days'
),
(
    'eeee2222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'SPEC_FETCHED',
    '{"specSize": 125000, "endpoints": 20}'::jsonb,
    NULL,
    NOW() - INTERVAL '3 days' + INTERVAL '30 seconds'
),
(
    'eeee3333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    'SCENARIO_CREATED',
    '{"scenarioCount": 12}'::jsonb,
    NULL,
    NOW() - INTERVAL '3 days' + INTERVAL '2 minutes'
),
(
    'eeee4444-4444-4444-4444-444444444444',
    '11111111-1111-1111-1111-111111111111',
    'COMPLETE',
    '{"totalDuration": 900000, "passed": 10, "failed": 2}'::jsonb,
    NULL,
    NOW() - INTERVAL '1 hour 45 minutes'
)
ON CONFLICT (id) DO UPDATE SET
    event_type = EXCLUDED.event_type;

-- ============================================================================
-- SAMPLE AI LOGS
-- ============================================================================

INSERT INTO ai_logs (
    id, correlation_id, provider, model, prompt_type,
    prompt_tokens, completion_tokens, total_tokens,
    prompt_text, response_text, status, duration_ms,
    user_id, created_at
) VALUES
(
    'ffff1111-1111-1111-1111-111111111111',
    '11111111-1111-1111-1111-111111111111',
    'openai',
    'gpt-4-turbo',
    'SCENARIO_GENERATION',
    2500,
    3200,
    5700,
    'Generate test scenarios for the following OpenAPI specification...',
    '[{"name": "Create and retrieve a pet", "steps": [...]}]',
    'SUCCESS',
    4500,
    'user-001',
    NOW() - INTERVAL '3 days' + INTERVAL '1 minute'
),
(
    'ffff2222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'openai',
    'gpt-4-turbo',
    'QA_EVALUATION',
    1800,
    2100,
    3900,
    'Evaluate the following test results and provide a QA summary...',
    '{"summary": "Test suite completed with 83% pass rate...", "recommendations": [...]}',
    'SUCCESS',
    3200,
    NULL,
    NOW() - INTERVAL '1 hour 45 minutes'
)
ON CONFLICT (id) DO UPDATE SET
    status = EXCLUDED.status;

-- ============================================================================
-- VERIFICATION
-- ============================================================================

DO $$
DECLARE
    pkg_count INTEGER;
    scenario_count INTEGER;
    run_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO pkg_count FROM qa_packages;
    SELECT COUNT(*) INTO scenario_count FROM test_scenarios;
    SELECT COUNT(*) INTO run_count FROM test_runs;

    RAISE NOTICE '✅ Seed data loaded successfully!';
    RAISE NOTICE '   - QA Packages: %', pkg_count;
    RAISE NOTICE '   - Test Scenarios: %', scenario_count;
    RAISE NOTICE '   - Test Runs: %', run_count;
END $$;
