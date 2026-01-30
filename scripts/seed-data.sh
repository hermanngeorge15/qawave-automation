#!/bin/bash
# seed-data.sh - Seed local development database with sample data
# Author: Database Agent
# Date: 2026-01-30
#
# Usage:
#   ./scripts/seed-data.sh
#   make db-seed

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SEED_FILE="${SCRIPT_DIR}/seed.sql"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}🌱 Seeding QAWave database...${NC}"

# Check if seed file exists
if [ ! -f "$SEED_FILE" ]; then
    echo -e "${RED}❌ Seed file not found: ${SEED_FILE}${NC}"
    exit 1
fi

# Determine how to connect to PostgreSQL
if command -v docker &> /dev/null && docker ps --format '{{.Names}}' | grep -q postgres; then
    # Docker container available
    echo -e "${YELLOW}📦 Using Docker container...${NC}"
    docker compose exec -T postgres psql -U qawave -d qawave < "$SEED_FILE"
elif [ -n "$DATABASE_URL" ]; then
    # Environment variable set
    echo -e "${YELLOW}🔗 Using DATABASE_URL...${NC}"
    psql "$DATABASE_URL" < "$SEED_FILE"
elif command -v psql &> /dev/null; then
    # Local psql available
    echo -e "${YELLOW}🖥️  Using local psql...${NC}"
    PGHOST=${PGHOST:-localhost}
    PGPORT=${PGPORT:-5432}
    PGUSER=${PGUSER:-qawave}
    PGDATABASE=${PGDATABASE:-qawave}

    psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" < "$SEED_FILE"
else
    echo -e "${RED}❌ No PostgreSQL client found. Please install psql or run via Docker.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Database seeded successfully!${NC}"
echo ""
echo "Sample data created:"
echo "  - 5 QA Packages (various statuses)"
echo "  - 6 Test Scenarios"
echo "  - 3 Test Runs"
echo "  - Step results and events"
echo ""
echo "Sample users for testing:"
echo "  - tester@qawave.local (user-001)"
echo "  - admin@qawave.local (user-002)"
