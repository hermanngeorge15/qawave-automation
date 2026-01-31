#!/bin/bash
# ==============================================================================
# QAWave Local Development Health Check
# ==============================================================================
# Verifies all local development services are running and healthy.
# Usage: ./scripts/health-check-local.sh [--quiet] [--json]
#
# Options:
#   --quiet   Only show failures, suppress success messages
#   --json    Output in JSON format (for CI/scripts)
#
# Exit codes:
#   0 - All required services healthy
#   1 - One or more required services unhealthy
#   2 - Script error
# ==============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
POSTGRES_HOST="${DB_HOST:-localhost}"
POSTGRES_PORT="${DB_PORT:-5432}"
POSTGRES_USER="${DB_USER:-qawave}"
REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"
KAFKA_HOST="${KAFKA_HOST:-localhost}"
KAFKA_PORT="${KAFKA_PORT:-9094}"
BACKEND_HOST="${BACKEND_HOST:-localhost}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_HOST="${FRONTEND_HOST:-localhost}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"

# Parse arguments
QUIET=false
JSON_OUTPUT=false

for arg in "$@"; do
    case $arg in
        --quiet|-q)
            QUIET=true
            ;;
        --json)
            JSON_OUTPUT=true
            ;;
        --help|-h)
            echo "Usage: $0 [--quiet] [--json]"
            echo ""
            echo "Options:"
            echo "  --quiet, -q   Only show failures"
            echo "  --json        Output in JSON format"
            exit 0
            ;;
    esac
done

# Tracking
REQUIRED_PASSED=0
REQUIRED_FAILED=0
OPTIONAL_PASSED=0
OPTIONAL_FAILED=0

declare -A RESULTS

# ==============================================================================
# Helper Functions
# ==============================================================================

log_success() {
    local name="$1"
    local message="$2"
    RESULTS["$name"]="success:$message"
    if [ "$JSON_OUTPUT" = false ] && [ "$QUIET" = false ]; then
        echo -e "  ${GREEN}✅${NC} $name - $message"
    fi
}

log_failure() {
    local name="$1"
    local message="$2"
    RESULTS["$name"]="failure:$message"
    if [ "$JSON_OUTPUT" = false ]; then
        echo -e "  ${RED}❌${NC} $name - $message"
    fi
}

log_warning() {
    local name="$1"
    local message="$2"
    RESULTS["$name"]="warning:$message"
    if [ "$JSON_OUTPUT" = false ] && [ "$QUIET" = false ]; then
        echo -e "  ${YELLOW}⚠️${NC}  $name - $message"
    fi
}

log_info() {
    if [ "$JSON_OUTPUT" = false ] && [ "$QUIET" = false ]; then
        echo -e "${BLUE}$1${NC}"
    fi
}

# Check if a port is open
check_port() {
    local host="$1"
    local port="$2"
    nc -z -w 2 "$host" "$port" 2>/dev/null
}

# Check if Docker container is healthy
check_docker_health() {
    local container="$1"
    local health
    health=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "not_found")
    [ "$health" = "healthy" ]
}

# Check if Docker container is running
check_docker_running() {
    local container="$1"
    local state
    state=$(docker inspect --format='{{.State.Status}}' "$container" 2>/dev/null || echo "not_found")
    [ "$state" = "running" ]
}

# ==============================================================================
# Service Checks
# ==============================================================================

check_postgresql() {
    local name="PostgreSQL"
    local addr="$POSTGRES_HOST:$POSTGRES_PORT"

    # Try Docker container first
    if check_docker_running "qawave-postgres"; then
        if check_docker_health "qawave-postgres"; then
            local tables
            tables=$(docker exec qawave-postgres psql -U "$POSTGRES_USER" -d qawave -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public'" 2>/dev/null | tr -d ' ' || echo "?")
            log_success "$name" "$addr (healthy, $tables tables)"
            return 0
        else
            log_failure "$name" "$addr (container unhealthy)"
            return 1
        fi
    fi

    # Try local pg_isready
    if command -v pg_isready &>/dev/null; then
        if pg_isready -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" &>/dev/null; then
            log_success "$name" "$addr (connected)"
            return 0
        fi
    fi

    # Try port check as fallback
    if check_port "$POSTGRES_HOST" "$POSTGRES_PORT"; then
        log_success "$name" "$addr (port open)"
        return 0
    fi

    log_failure "$name" "$addr (not available)"
    return 1
}

check_redis() {
    local name="Redis"
    local addr="$REDIS_HOST:$REDIS_PORT"

    # Try Docker container first
    if check_docker_running "qawave-redis"; then
        if check_docker_health "qawave-redis"; then
            local memory
            memory=$(docker exec qawave-redis redis-cli INFO memory 2>/dev/null | grep used_memory_human | cut -d: -f2 | tr -d '\r' || echo "?")
            log_success "$name" "$addr (healthy, ${memory})"
            return 0
        else
            log_failure "$name" "$addr (container unhealthy)"
            return 1
        fi
    fi

    # Try local redis-cli
    if command -v redis-cli &>/dev/null; then
        if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping &>/dev/null; then
            log_success "$name" "$addr (PONG)"
            return 0
        fi
    fi

    # Try port check as fallback
    if check_port "$REDIS_HOST" "$REDIS_PORT"; then
        log_success "$name" "$addr (port open)"
        return 0
    fi

    log_failure "$name" "$addr (not available)"
    return 1
}

check_kafka() {
    local name="Kafka"
    local addr="$KAFKA_HOST:$KAFKA_PORT"

    # Try Docker container first
    if check_docker_running "qawave-kafka"; then
        if check_docker_health "qawave-kafka"; then
            local topics
            topics=$(docker exec qawave-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list 2>/dev/null | wc -l | tr -d ' ' || echo "?")
            log_success "$name" "$addr (healthy, $topics topics)"
            return 0
        elif check_docker_running "qawave-kafka"; then
            # Container running but health check not passing (might still be starting)
            log_warning "$name" "$addr (starting...)"
            return 0
        fi
    fi

    # Try port check
    if check_port "$KAFKA_HOST" "$KAFKA_PORT"; then
        log_success "$name" "$addr (port open)"
        return 0
    fi

    log_failure "$name" "$addr (not available)"
    return 1
}

check_backend() {
    local name="Backend"
    local addr="$BACKEND_HOST:$BACKEND_PORT"

    # Try health endpoint
    local response
    response=$(curl -sf "http://$addr/actuator/health" 2>/dev/null || curl -sf "http://$addr/health" 2>/dev/null || echo "")

    if [ -n "$response" ]; then
        local status
        status=$(echo "$response" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4 || echo "UP")
        if [ "$status" = "UP" ] || [ -n "$response" ]; then
            log_success "$name" "$addr (healthy)"
            return 0
        fi
    fi

    # Try basic HTTP check
    if curl -sf -o /dev/null "http://$addr" 2>/dev/null; then
        log_success "$name" "$addr (responding)"
        return 0
    fi

    # Try Docker container
    if check_docker_running "qawave-backend"; then
        log_warning "$name" "$addr (container running, waiting...)"
        return 0
    fi

    # Try port check
    if check_port "$BACKEND_HOST" "$BACKEND_PORT"; then
        log_warning "$name" "$addr (port open, not responding to HTTP)"
        return 0
    fi

    log_failure "$name" "$addr (not available)"
    return 1
}

check_frontend() {
    local name="Frontend"
    local addr="$FRONTEND_HOST:$FRONTEND_PORT"

    # Try HTTP check
    local http_code
    http_code=$(curl -sf -o /dev/null -w "%{http_code}" "http://$addr" 2>/dev/null || echo "000")

    if [ "$http_code" = "200" ] || [ "$http_code" = "304" ]; then
        log_success "$name" "$addr (serving)"
        return 0
    fi

    # Try Docker container
    if check_docker_running "qawave-frontend"; then
        log_success "$name" "$addr (container running)"
        return 0
    fi

    # Try port check
    if check_port "$FRONTEND_HOST" "$FRONTEND_PORT"; then
        log_success "$name" "$addr (port open)"
        return 0
    fi

    log_failure "$name" "$addr (not available)"
    return 1
}

# ==============================================================================
# Optional Service Checks
# ==============================================================================

check_kafka_ui() {
    local name="Kafka UI"
    local addr="localhost:8090"

    if check_docker_running "qawave-kafka-ui"; then
        local http_code
        http_code=$(curl -sf -o /dev/null -w "%{http_code}" "http://$addr" 2>/dev/null || echo "000")
        if [ "$http_code" = "200" ] || [ "$http_code" = "302" ]; then
            log_success "$name" "$addr (available)"
            return 0
        fi
        log_warning "$name" "$addr (starting...)"
        return 0
    fi

    log_warning "$name" "$addr (not running)"
    return 1
}

check_keycloak() {
    local name="Keycloak"
    local addr="localhost:8180"

    local http_code
    http_code=$(curl -sf -o /dev/null -w "%{http_code}" "http://$addr" 2>/dev/null || echo "000")

    if [ "$http_code" = "200" ] || [ "$http_code" = "302" ] || [ "$http_code" = "303" ]; then
        log_success "$name" "$addr (available)"
        return 0
    fi

    log_warning "$name" "$addr (not running)"
    return 1
}

# ==============================================================================
# JSON Output
# ==============================================================================

output_json() {
    local required_status="healthy"
    [ "$REQUIRED_FAILED" -gt 0 ] && required_status="unhealthy"

    echo "{"
    echo "  \"timestamp\": \"$(date -u +"%Y-%m-%dT%H:%M:%SZ")\","
    echo "  \"status\": \"$required_status\","
    echo "  \"required\": {"
    echo "    \"passed\": $REQUIRED_PASSED,"
    echo "    \"failed\": $REQUIRED_FAILED"
    echo "  },"
    echo "  \"optional\": {"
    echo "    \"passed\": $OPTIONAL_PASSED,"
    echo "    \"failed\": $OPTIONAL_FAILED"
    echo "  },"
    echo "  \"services\": {"

    local first=true
    for name in "${!RESULTS[@]}"; do
        local result="${RESULTS[$name]}"
        local status="${result%%:*}"
        local message="${result#*:}"

        [ "$first" = false ] && echo ","
        first=false

        echo -n "    \"$name\": {\"status\": \"$status\", \"message\": \"$message\"}"
    done

    echo ""
    echo "  }"
    echo "}"
}

# ==============================================================================
# Main
# ==============================================================================

main() {
    if [ "$JSON_OUTPUT" = false ]; then
        echo ""
        echo -e "${GREEN}🏥 QAWave Local Health Check${NC}"
        echo "=============================="
        echo ""
    fi

    # Required Infrastructure
    if [ "$JSON_OUTPUT" = false ]; then
        log_info "Infrastructure:"
    fi

    if check_postgresql; then ((REQUIRED_PASSED++)); else ((REQUIRED_FAILED++)); fi
    if check_redis; then ((REQUIRED_PASSED++)); else ((REQUIRED_FAILED++)); fi
    if check_kafka; then ((REQUIRED_PASSED++)); else ((REQUIRED_FAILED++)); fi

    if [ "$JSON_OUTPUT" = false ]; then
        echo ""
        log_info "Applications:"
    fi

    if check_backend; then ((REQUIRED_PASSED++)); else ((REQUIRED_FAILED++)); fi
    if check_frontend; then ((REQUIRED_PASSED++)); else ((REQUIRED_FAILED++)); fi

    if [ "$JSON_OUTPUT" = false ]; then
        echo ""
        log_info "Optional Services:"
    fi

    if check_kafka_ui; then ((OPTIONAL_PASSED++)); else ((OPTIONAL_FAILED++)); fi
    if check_keycloak; then ((OPTIONAL_PASSED++)); else ((OPTIONAL_FAILED++)); fi

    # Output
    if [ "$JSON_OUTPUT" = true ]; then
        output_json
    else
        echo ""
        echo "=============================="
        local total_required=$((REQUIRED_PASSED + REQUIRED_FAILED))

        if [ "$REQUIRED_FAILED" -eq 0 ]; then
            echo -e "${GREEN}Overall Status: ✅ HEALTHY${NC} ($REQUIRED_PASSED/$total_required required services)"
        else
            echo -e "${RED}Overall Status: ❌ UNHEALTHY${NC} ($REQUIRED_PASSED/$total_required required services)"
        fi
        echo "=============================="
        echo ""
    fi

    # Exit with appropriate code
    if [ "$REQUIRED_FAILED" -gt 0 ]; then
        exit 1
    fi
    exit 0
}

main "$@"
