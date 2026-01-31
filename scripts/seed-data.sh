#!/bin/bash
# ==============================================================================
# QAWave Database Seed Script
# ==============================================================================
# Seeds the local database with test data for development.
# Usage: ./scripts/seed-data.sh [--reset]
#
# Options:
#   --reset   Clear existing data before seeding
# ==============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-qawave}"
DB_USER="${DB_USER:-qawave}"
DB_PASSWORD="${DB_PASSWORD:-qawave_dev_password}"

RESET=false

for arg in "$@"; do
    case $arg in
        --reset)
            RESET=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [--reset]"
            echo ""
            echo "Options:"
            echo "  --reset   Clear existing data before seeding"
            exit 0
            ;;
    esac
done

log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check database connectivity
check_database() {
    log_info "Checking database connectivity..."

    if command -v docker &> /dev/null; then
        if docker compose exec -T postgres pg_isready -U "$DB_USER" &> /dev/null; then
            log_success "Database is accessible"
            return 0
        fi
    fi

    if command -v psql &> /dev/null; then
        if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1" &> /dev/null; then
            log_success "Database is accessible"
            return 0
        fi
    fi

    log_error "Cannot connect to database"
    exit 1
}

# Execute SQL via docker compose or psql
execute_sql() {
    local sql="$1"

    if command -v docker &> /dev/null && docker compose ps postgres | grep -q "running"; then
        echo "$sql" | docker compose exec -T postgres psql -U "$DB_USER" -d "$DB_NAME" -q
    else
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -q -c "$sql"
    fi
}

# Reset data
reset_data() {
    log_warning "Resetting existing data..."

    execute_sql "
        TRUNCATE TABLE test_step_results CASCADE;
        TRUNCATE TABLE test_run_results CASCADE;
        TRUNCATE TABLE test_runs CASCADE;
        TRUNCATE TABLE test_steps CASCADE;
        TRUNCATE TABLE scenarios CASCADE;
        TRUNCATE TABLE packages CASCADE;
        TRUNCATE TABLE users CASCADE;
    " 2>/dev/null || log_warning "Some tables may not exist yet"

    log_success "Data reset complete"
}

# Seed users
seed_users() {
    log_info "Seeding users..."

    execute_sql "
        INSERT INTO users (id, email, name, role, created_at, updated_at)
        VALUES
            ('11111111-1111-1111-1111-111111111111', 'admin@qawave.local', 'Admin User', 'admin', NOW(), NOW()),
            ('22222222-2222-2222-2222-222222222222', 'tester@qawave.local', 'Test User', 'tester', NOW(), NOW()),
            ('33333333-3333-3333-3333-333333333333', 'viewer@qawave.local', 'Viewer User', 'viewer', NOW(), NOW())
        ON CONFLICT (id) DO NOTHING;
    " 2>/dev/null || log_warning "Users table may not exist"

    log_success "Users seeded"
}

# Seed packages
seed_packages() {
    log_info "Seeding packages..."

    execute_sql "
        INSERT INTO packages (id, name, description, base_url, created_by, created_at, updated_at)
        VALUES
            ('aaaa1111-1111-1111-1111-111111111111', 'Demo API', 'Demo API for testing', 'https://jsonplaceholder.typicode.com', '11111111-1111-1111-1111-111111111111', NOW(), NOW()),
            ('aaaa2222-2222-2222-2222-222222222222', 'Pet Store API', 'Swagger Petstore example', 'https://petstore.swagger.io/v2', '11111111-1111-1111-1111-111111111111', NOW(), NOW()),
            ('aaaa3333-3333-3333-3333-333333333333', 'HTTPBin', 'HTTP testing service', 'https://httpbin.org', '22222222-2222-2222-2222-222222222222', NOW(), NOW())
        ON CONFLICT (id) DO NOTHING;
    " 2>/dev/null || log_warning "Packages table may not exist"

    log_success "Packages seeded"
}

# Seed scenarios
seed_scenarios() {
    log_info "Seeding scenarios..."

    execute_sql "
        INSERT INTO scenarios (id, package_id, name, description, method, endpoint, created_at, updated_at)
        VALUES
            -- Demo API scenarios
            ('bbbb1111-1111-1111-1111-111111111111', 'aaaa1111-1111-1111-1111-111111111111', 'Get Posts', 'Retrieve all posts', 'GET', '/posts', NOW(), NOW()),
            ('bbbb1112-1111-1111-1111-111111111111', 'aaaa1111-1111-1111-1111-111111111111', 'Get Single Post', 'Retrieve post by ID', 'GET', '/posts/1', NOW(), NOW()),
            ('bbbb1113-1111-1111-1111-111111111111', 'aaaa1111-1111-1111-1111-111111111111', 'Create Post', 'Create a new post', 'POST', '/posts', NOW(), NOW()),

            -- Pet Store scenarios
            ('bbbb2221-2222-2222-2222-222222222222', 'aaaa2222-2222-2222-2222-222222222222', 'Get Pet by ID', 'Retrieve pet by ID', 'GET', '/pet/{petId}', NOW(), NOW()),
            ('bbbb2222-2222-2222-2222-222222222222', 'aaaa2222-2222-2222-2222-222222222222', 'Add New Pet', 'Add a new pet to the store', 'POST', '/pet', NOW(), NOW()),

            -- HTTPBin scenarios
            ('bbbb3331-3333-3333-3333-333333333333', 'aaaa3333-3333-3333-3333-333333333333', 'Test GET', 'Test GET request', 'GET', '/get', NOW(), NOW()),
            ('bbbb3332-3333-3333-3333-333333333333', 'aaaa3333-3333-3333-3333-333333333333', 'Test POST', 'Test POST request', 'POST', '/post', NOW(), NOW())
        ON CONFLICT (id) DO NOTHING;
    " 2>/dev/null || log_warning "Scenarios table may not exist"

    log_success "Scenarios seeded"
}

# Seed test runs
seed_test_runs() {
    log_info "Seeding test runs..."

    execute_sql "
        INSERT INTO test_runs (id, package_id, status, started_at, completed_at, created_by, created_at)
        VALUES
            ('cccc1111-1111-1111-1111-111111111111', 'aaaa1111-1111-1111-1111-111111111111', 'completed', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day' + INTERVAL '5 minutes', '22222222-2222-2222-2222-222222222222', NOW() - INTERVAL '1 day'),
            ('cccc1112-1111-1111-1111-111111111112', 'aaaa1111-1111-1111-1111-111111111111', 'completed', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours' + INTERVAL '3 minutes', '22222222-2222-2222-2222-222222222222', NOW() - INTERVAL '2 hours'),
            ('cccc2221-2222-2222-2222-222222222222', 'aaaa2222-2222-2222-2222-222222222222', 'running', NOW() - INTERVAL '10 minutes', NULL, '11111111-1111-1111-1111-111111111111', NOW() - INTERVAL '10 minutes'),
            ('cccc3331-3333-3333-3333-333333333333', 'aaaa3333-3333-3333-3333-333333333333', 'failed', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days' + INTERVAL '2 minutes', '22222222-2222-2222-2222-222222222222', NOW() - INTERVAL '3 days')
        ON CONFLICT (id) DO NOTHING;
    " 2>/dev/null || log_warning "Test runs table may not exist"

    log_success "Test runs seeded"
}

# Main
main() {
    echo ""
    echo -e "${GREEN}🌱 QAWave Database Seed${NC}"
    echo "========================"
    echo ""

    check_database

    if [ "$RESET" = true ]; then
        reset_data
    fi

    seed_users
    seed_packages
    seed_scenarios
    seed_test_runs

    echo ""
    log_success "Database seeding complete!"
    echo ""
    echo -e "${BLUE}Seeded data summary:${NC}"
    echo "  - 3 users (admin, tester, viewer)"
    echo "  - 3 packages (Demo API, Pet Store, HTTPBin)"
    echo "  - 7 scenarios"
    echo "  - 4 test runs"
    echo ""
}

main
