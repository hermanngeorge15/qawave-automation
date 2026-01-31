#!/bin/bash
# ==============================================================================
# QAWave Local Development Setup Script
# ==============================================================================
# This script sets up the local development environment from scratch.
# Usage: ./scripts/setup-local.sh [--skip-deps] [--skip-docker] [--reset]
#
# Options:
#   --skip-deps    Skip dependency installation
#   --skip-docker  Skip Docker infrastructure startup
#   --reset        Reset everything (remove volumes, reinstall)
# ==============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
REQUIRED_DOCKER_VERSION="20.0.0"
REQUIRED_NODE_VERSION="20"
REQUIRED_JAVA_VERSION="21"

# Parse arguments
SKIP_DEPS=false
SKIP_DOCKER=false
RESET=false

for arg in "$@"; do
    case $arg in
        --skip-deps)
            SKIP_DEPS=true
            shift
            ;;
        --skip-docker)
            SKIP_DOCKER=true
            shift
            ;;
        --reset)
            RESET=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [--skip-deps] [--skip-docker] [--reset]"
            echo ""
            echo "Options:"
            echo "  --skip-deps    Skip dependency installation"
            echo "  --skip-docker  Skip Docker infrastructure startup"
            echo "  --reset        Reset everything (remove volumes, reinstall)"
            exit 0
            ;;
    esac
done

# ==============================================================================
# Helper Functions
# ==============================================================================

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

check_command() {
    if ! command -v "$1" &> /dev/null; then
        log_error "$1 is required but not installed"
        return 1
    fi
    return 0
}

version_gte() {
    # Compare versions: returns 0 if $1 >= $2
    printf '%s\n%s\n' "$2" "$1" | sort -V -C
}

# ==============================================================================
# Prerequisites Check
# ==============================================================================

check_prerequisites() {
    echo ""
    echo -e "${BLUE}🔍 Checking Prerequisites${NC}"
    echo "=========================="
    local all_good=true

    # Check Docker
    if check_command docker; then
        DOCKER_VERSION=$(docker version --format '{{.Server.Version}}' 2>/dev/null || echo "0.0.0")
        if version_gte "$DOCKER_VERSION" "$REQUIRED_DOCKER_VERSION"; then
            log_success "Docker $DOCKER_VERSION"
        else
            log_warning "Docker $DOCKER_VERSION (recommended: $REQUIRED_DOCKER_VERSION+)"
        fi
    else
        log_error "Docker is not installed"
        echo "       Install from: https://docs.docker.com/get-docker/"
        all_good=false
    fi

    # Check Docker Compose
    if docker compose version &> /dev/null; then
        COMPOSE_VERSION=$(docker compose version --short)
        log_success "Docker Compose $COMPOSE_VERSION"
    else
        log_error "Docker Compose is not available"
        echo "       Ensure you have Docker Desktop or docker-compose-plugin installed"
        all_good=false
    fi

    # Check Node.js
    if check_command node; then
        NODE_VERSION=$(node -v | sed 's/v//' | cut -d. -f1)
        if [ "$NODE_VERSION" -ge "$REQUIRED_NODE_VERSION" ]; then
            log_success "Node.js $(node -v)"
        else
            log_warning "Node.js $(node -v) (recommended: v$REQUIRED_NODE_VERSION+)"
        fi
    else
        log_error "Node.js is not installed"
        echo "       Install from: https://nodejs.org/"
        all_good=false
    fi

    # Check npm
    if check_command npm; then
        log_success "npm $(npm -v)"
    else
        log_error "npm is not installed"
        all_good=false
    fi

    # Check Java
    if check_command java; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge "$REQUIRED_JAVA_VERSION" ]; then
            log_success "Java $(java -version 2>&1 | head -n 1 | cut -d'"' -f2)"
        else
            log_warning "Java version $JAVA_VERSION (recommended: $REQUIRED_JAVA_VERSION+)"
        fi
    else
        log_error "Java is not installed"
        echo "       Install JDK 21 from: https://adoptium.net/"
        all_good=false
    fi

    # Check Git
    if check_command git; then
        log_success "Git $(git --version | cut -d' ' -f3)"
    else
        log_warning "Git is not installed (optional)"
    fi

    if [ "$all_good" = false ]; then
        echo ""
        log_error "Please install missing dependencies and try again"
        exit 1
    fi

    echo ""
}

# ==============================================================================
# Environment Setup
# ==============================================================================

setup_environment() {
    echo -e "${BLUE}📝 Setting Up Environment${NC}"
    echo "=========================="

    if [ ! -f .env ]; then
        if [ -f .env.example ]; then
            cp .env.example .env
            log_success "Created .env from .env.example"
            log_warning "Please update .env with your AI provider API key"
        else
            log_error ".env.example not found"
            exit 1
        fi
    else
        log_info ".env already exists, skipping"
    fi

    echo ""
}

# ==============================================================================
# Dependency Installation
# ==============================================================================

install_dependencies() {
    if [ "$SKIP_DEPS" = true ]; then
        log_info "Skipping dependency installation (--skip-deps)"
        return
    fi

    echo -e "${BLUE}📦 Installing Dependencies${NC}"
    echo "==========================="

    # Backend dependencies
    log_info "Installing backend dependencies..."
    if [ -f backend/gradlew ]; then
        (cd backend && ./gradlew dependencies --quiet) || log_warning "Backend dependency download had warnings"
        log_success "Backend dependencies ready"
    else
        log_warning "Backend gradlew not found, skipping"
    fi

    # Frontend dependencies
    log_info "Installing frontend dependencies..."
    if [ -f frontend/package.json ]; then
        (cd frontend && npm ci --silent) || (cd frontend && npm install --silent)
        log_success "Frontend dependencies installed"
    else
        log_warning "Frontend package.json not found, skipping"
    fi

    # E2E test dependencies
    if [ -f e2e-tests/package.json ]; then
        log_info "Installing E2E test dependencies..."
        (cd e2e-tests && npm ci --silent) || (cd e2e-tests && npm install --silent)
        log_success "E2E dependencies installed"
    fi

    echo ""
}

# ==============================================================================
# Docker Infrastructure
# ==============================================================================

start_infrastructure() {
    if [ "$SKIP_DOCKER" = true ]; then
        log_info "Skipping Docker infrastructure (--skip-docker)"
        return
    fi

    echo -e "${BLUE}🐳 Starting Infrastructure${NC}"
    echo "==========================="

    # Check if Docker is running
    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running"
        echo "       Please start Docker Desktop or the Docker service"
        exit 1
    fi

    # Reset if requested
    if [ "$RESET" = true ]; then
        log_warning "Resetting infrastructure (removing volumes)..."
        docker compose down -v --remove-orphans 2>/dev/null || true
    fi

    # Start services
    log_info "Starting PostgreSQL, Redis, and Kafka..."
    docker compose up -d postgres redis kafka

    # Wait for services
    log_info "Waiting for services to be healthy..."

    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if docker compose ps | grep -q "healthy"; then
            postgres_healthy=$(docker compose ps postgres | grep -c "healthy" || echo "0")
            redis_healthy=$(docker compose ps redis | grep -c "healthy" || echo "0")
            kafka_healthy=$(docker compose ps kafka | grep -c "healthy" || echo "0")

            if [ "$postgres_healthy" -ge 1 ] && [ "$redis_healthy" -ge 1 ]; then
                break
            fi
        fi

        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    echo ""

    # Verify services
    if docker compose ps postgres | grep -q "healthy"; then
        log_success "PostgreSQL is healthy"
    else
        log_warning "PostgreSQL may not be fully ready"
    fi

    if docker compose ps redis | grep -q "healthy"; then
        log_success "Redis is healthy"
    else
        log_warning "Redis may not be fully ready"
    fi

    if docker compose ps kafka | grep -q "healthy"; then
        log_success "Kafka is healthy"
    else
        log_warning "Kafka may still be starting up"
    fi

    echo ""
}

# ==============================================================================
# Health Check
# ==============================================================================

run_health_check() {
    echo -e "${BLUE}🏥 Running Health Checks${NC}"
    echo "========================="

    if [ -f scripts/health-check.sh ]; then
        ./scripts/health-check.sh --quiet || log_warning "Some health checks failed"
    else
        # Basic connectivity checks
        log_info "Checking PostgreSQL..."
        if docker compose exec -T postgres pg_isready -U qawave &> /dev/null; then
            log_success "PostgreSQL accepting connections"
        else
            log_warning "PostgreSQL not ready"
        fi

        log_info "Checking Redis..."
        if docker compose exec -T redis redis-cli ping &> /dev/null; then
            log_success "Redis responding"
        else
            log_warning "Redis not ready"
        fi
    fi

    echo ""
}

# ==============================================================================
# Summary
# ==============================================================================

print_summary() {
    echo ""
    echo -e "${GREEN}🎉 Setup Complete!${NC}"
    echo "=================="
    echo ""
    echo -e "${BLUE}Service URLs:${NC}"
    echo "  PostgreSQL:  localhost:5432"
    echo "  Redis:       localhost:6379"
    echo "  Kafka:       localhost:9094"
    echo ""
    echo -e "${BLUE}Next Steps:${NC}"
    echo "  1. Update .env with your AI provider API key"
    echo "  2. Start the backend:  make backend-run"
    echo "  3. Start the frontend: make frontend-run"
    echo ""
    echo -e "${BLUE}Useful Commands:${NC}"
    echo "  make help         Show all available commands"
    echo "  make status       Check service status"
    echo "  make logs         View service logs"
    echo "  make down         Stop all services"
    echo ""
    echo -e "${YELLOW}Documentation:${NC} docs/agents/DEVOPS.md"
    echo ""
}

# ==============================================================================
# Main
# ==============================================================================

main() {
    echo ""
    echo -e "${GREEN}🚀 QAWave Local Development Setup${NC}"
    echo "===================================="
    echo ""

    check_prerequisites
    setup_environment
    install_dependencies
    start_infrastructure
    run_health_check
    print_summary
}

# Run main
main
