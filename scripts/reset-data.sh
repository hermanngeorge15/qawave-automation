#!/bin/bash
# reset-data.sh - Reset local development database (clear all data)
# Author: Database Agent
# Date: 2026-01-30
#
# WARNING: This will DELETE all data from the database!
#
# Usage:
#   ./scripts/reset-data.sh
#   make db-reset

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${RED}⚠️  WARNING: This will DELETE all data from the database!${NC}"
echo ""

# Prompt for confirmation
read -p "Are you sure you want to continue? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo -e "${YELLOW}Cancelled.${NC}"
    exit 0
fi

echo -e "${YELLOW}🧹 Resetting QAWave database...${NC}"

RESET_SQL="
TRUNCATE
    qa_package_events,
    ai_logs,
    test_step_results,
    test_runs,
    test_scenarios,
    qa_packages
CASCADE;

-- Reset sequences if any
-- Note: UUIDs are used, so no sequences to reset

SELECT 'Database reset complete' as status;
"

# Determine how to connect to PostgreSQL
if command -v docker &> /dev/null && docker ps --format '{{.Names}}' | grep -q postgres; then
    echo -e "${YELLOW}📦 Using Docker container...${NC}"
    echo "$RESET_SQL" | docker compose exec -T postgres psql -U qawave -d qawave
elif [ -n "$DATABASE_URL" ]; then
    echo -e "${YELLOW}🔗 Using DATABASE_URL...${NC}"
    echo "$RESET_SQL" | psql "$DATABASE_URL"
elif command -v psql &> /dev/null; then
    echo -e "${YELLOW}🖥️  Using local psql...${NC}"
    PGHOST=${PGHOST:-localhost}
    PGPORT=${PGPORT:-5432}
    PGUSER=${PGUSER:-qawave}
    PGDATABASE=${PGDATABASE:-qawave}

    echo "$RESET_SQL" | psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE"
else
    echo -e "${RED}❌ No PostgreSQL client found.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Database reset complete!${NC}"
echo ""
echo "To seed with sample data, run:"
echo "  ./scripts/seed-data.sh"
echo "  or: make db-seed"
