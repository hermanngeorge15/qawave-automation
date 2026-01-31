# ==============================================================================
# QAWave Development Makefile
# ==============================================================================
# Usage: make [target]
# Run 'make help' to see all available targets
#
# Quick Start:
#   make setup    # First-time setup
#   make up       # Start development environment
#   make down     # Stop everything
# ==============================================================================

.PHONY: help setup up down restart logs clean \
        infra-up infra-down infra-logs infra-clean infra-status \
        backend-run backend-test backend-build backend-docker backend-lint \
        frontend-install frontend-run frontend-test frontend-build frontend-lint \
        test test-unit test-integration test-e2e test-coverage \
        db-migrate db-seed db-reset db-shell \
        docker-build docker-push \
        check-deps health health-check-local health-check-local-quiet \
        health-check-json health-check-staging

# Default target
.DEFAULT_GOAL := help

# Colors for output
BLUE := \033[34m
GREEN := \033[32m
YELLOW := \033[33m
RED := \033[31m
NC := \033[0m # No Color

# ==============================================================================
# Configuration
# ==============================================================================

# Docker compose files
COMPOSE_FILE := docker-compose.yml
COMPOSE_CMD := docker compose -f $(COMPOSE_FILE)

# Project directories
BACKEND_DIR := backend
FRONTEND_DIR := frontend
E2E_DIR := e2e-tests

# ==============================================================================
# Help
# ==============================================================================

help: ## Show this help message
	@echo ""
	@echo "$(BLUE)QAWave Development Commands$(NC)"
	@echo "============================="
	@echo ""
	@echo "$(GREEN)Quick Start:$(NC)"
	@echo "  make setup          First-time project setup"
	@echo "  make up             Start development environment"
	@echo "  make down           Stop all services"
	@echo ""
	@awk 'BEGIN {FS = ":.*##"; printf ""} /^[a-zA-Z_-]+:.*?##/ { printf "  $(BLUE)%-18s$(NC) %s\n", $$1, $$2 }' $(MAKEFILE_LIST)
	@echo ""

# ==============================================================================
# Setup & Prerequisites
# ==============================================================================

setup: check-deps ## First-time project setup
	@echo "$(GREEN)🚀 Setting up QAWave development environment...$(NC)"
	@./scripts/setup-local.sh
	@echo "$(GREEN)✅ Setup complete! Run 'make up' to start.$(NC)"

check-deps: ## Check required dependencies
	@echo "$(BLUE)Checking dependencies...$(NC)"
	@command -v docker >/dev/null 2>&1 || { echo "$(RED)❌ Docker is required$(NC)"; exit 1; }
	@command -v java >/dev/null 2>&1 || { echo "$(RED)❌ Java 21+ is required$(NC)"; exit 1; }
	@command -v node >/dev/null 2>&1 || { echo "$(RED)❌ Node.js 20+ is required$(NC)"; exit 1; }
	@echo "$(GREEN)✅ All dependencies installed$(NC)"

# ==============================================================================
# Full Stack Commands
# ==============================================================================

up: infra-up ## Start all infrastructure services
	@echo "$(GREEN)✅ Infrastructure started$(NC)"
	@echo "$(BLUE)Services:$(NC)"
	@echo "  PostgreSQL: localhost:5432"
	@echo "  Redis:      localhost:6379"
	@echo "  Kafka:      localhost:9094"
	@echo ""
	@echo "$(YELLOW)To start backend:  make backend-run$(NC)"
	@echo "$(YELLOW)To start frontend: make frontend-run$(NC)"

up-full: ## Start everything including backend and frontend containers
	@echo "$(GREEN)Starting full stack...$(NC)"
	$(COMPOSE_CMD) --profile full up -d
	@make health-check

down: ## Stop all services
	@echo "$(YELLOW)Stopping all services...$(NC)"
	$(COMPOSE_CMD) --profile full --profile debug down
	@echo "$(GREEN)✅ All services stopped$(NC)"

restart: down up ## Restart all services

logs: ## Show logs for all services
	$(COMPOSE_CMD) logs -f

status: ## Show status of all services
	@echo "$(BLUE)Service Status:$(NC)"
	@$(COMPOSE_CMD) ps
	@echo ""
	@make health-check

clean: infra-clean ## Full cleanup (removes volumes and built artifacts)
	@echo "$(YELLOW)Cleaning build artifacts...$(NC)"
	@rm -rf $(BACKEND_DIR)/build
	@rm -rf $(FRONTEND_DIR)/dist $(FRONTEND_DIR)/node_modules
	@echo "$(GREEN)✅ Cleanup complete$(NC)"

# ==============================================================================
# Infrastructure Commands
# ==============================================================================

infra-up: ## Start infrastructure (PostgreSQL, Redis, Kafka)
	@echo "$(GREEN)Starting infrastructure...$(NC)"
	$(COMPOSE_CMD) up -d postgres redis kafka
	@echo "$(BLUE)Waiting for services to be healthy...$(NC)"
	@sleep 5
	@$(COMPOSE_CMD) ps

infra-down: ## Stop infrastructure
	$(COMPOSE_CMD) stop postgres redis kafka

infra-logs: ## Show infrastructure logs
	$(COMPOSE_CMD) logs -f postgres redis kafka

infra-clean: ## Remove infrastructure containers and volumes
	@echo "$(RED)⚠️  This will delete all data!$(NC)"
	$(COMPOSE_CMD) down -v --remove-orphans
	@echo "$(GREEN)✅ Infrastructure cleaned$(NC)"

infra-status: ## Show infrastructure status
	@echo "$(BLUE)Infrastructure Status:$(NC)"
	@docker ps --filter "name=qawave-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

infra-debug: ## Start infrastructure with debug tools (Kafka UI)
	$(COMPOSE_CMD) --profile debug up -d
	@echo "$(GREEN)Kafka UI available at: http://localhost:8090$(NC)"

# ==============================================================================
# Backend Commands
# ==============================================================================

backend-run: ## Run backend locally (requires infra-up)
	@echo "$(GREEN)Starting backend...$(NC)"
	cd $(BACKEND_DIR) && ./gradlew bootRun --args='--spring.profiles.active=local'

backend-test: ## Run backend tests
	@echo "$(BLUE)Running backend tests...$(NC)"
	cd $(BACKEND_DIR) && ./gradlew test

backend-test-integration: ## Run backend integration tests
	@echo "$(BLUE)Running integration tests...$(NC)"
	cd $(BACKEND_DIR) && ./gradlew integrationTest

backend-build: ## Build backend JAR
	@echo "$(BLUE)Building backend...$(NC)"
	cd $(BACKEND_DIR) && ./gradlew build -x test

backend-docker: ## Build backend Docker image
	@echo "$(BLUE)Building backend Docker image...$(NC)"
	docker build -t qawave/backend:local $(BACKEND_DIR)

backend-lint: ## Run backend linting (ktlint)
	cd $(BACKEND_DIR) && ./gradlew ktlintCheck

backend-lint-fix: ## Fix backend linting issues
	cd $(BACKEND_DIR) && ./gradlew ktlintFormat

backend-clean: ## Clean backend build
	cd $(BACKEND_DIR) && ./gradlew clean

# ==============================================================================
# Frontend Commands
# ==============================================================================

frontend-install: ## Install frontend dependencies
	@echo "$(BLUE)Installing frontend dependencies...$(NC)"
	cd $(FRONTEND_DIR) && npm ci

frontend-run: ## Run frontend dev server
	@echo "$(GREEN)Starting frontend...$(NC)"
	cd $(FRONTEND_DIR) && npm run dev

frontend-test: ## Run frontend tests
	@echo "$(BLUE)Running frontend tests...$(NC)"
	cd $(FRONTEND_DIR) && npm run test

frontend-test-watch: ## Run frontend tests in watch mode
	cd $(FRONTEND_DIR) && npm run test:watch

frontend-build: ## Build frontend for production
	@echo "$(BLUE)Building frontend...$(NC)"
	cd $(FRONTEND_DIR) && npm run build

frontend-docker: ## Build frontend Docker image
	@echo "$(BLUE)Building frontend Docker image...$(NC)"
	docker build -t qawave/frontend:local $(FRONTEND_DIR)

frontend-lint: ## Run frontend linting
	cd $(FRONTEND_DIR) && npm run lint

frontend-lint-fix: ## Fix frontend linting issues
	cd $(FRONTEND_DIR) && npm run lint:fix

frontend-typecheck: ## Run TypeScript type checking
	cd $(FRONTEND_DIR) && npm run typecheck

frontend-clean: ## Clean frontend build
	rm -rf $(FRONTEND_DIR)/dist $(FRONTEND_DIR)/node_modules/.vite

# ==============================================================================
# Testing Commands
# ==============================================================================

test: backend-test frontend-test ## Run all unit tests

test-unit: test ## Alias for test

test-integration: backend-test-integration ## Run integration tests

test-e2e: ## Run E2E tests (requires full stack running)
	@echo "$(BLUE)Running E2E tests...$(NC)"
	cd $(E2E_DIR) && npm ci && npm run test

test-e2e-ui: ## Run E2E tests with UI
	cd $(E2E_DIR) && npm run test:ui

test-coverage: ## Run tests with coverage
	cd $(BACKEND_DIR) && ./gradlew test jacocoTestReport
	cd $(FRONTEND_DIR) && npm run test:coverage

# ==============================================================================
# Database Commands
# ==============================================================================

db-migrate: ## Run database migrations
	@echo "$(BLUE)Running migrations...$(NC)"
	cd $(BACKEND_DIR) && ./gradlew flywayMigrate

db-seed: ## Seed database with test data
	@echo "$(BLUE)Seeding database...$(NC)"
	@./scripts/seed-data.sh

db-reset: ## Reset database (drop and recreate)
	@echo "$(RED)⚠️  This will delete all data!$(NC)"
	@read -p "Are you sure? [y/N] " confirm && [ "$$confirm" = "y" ]
	$(COMPOSE_CMD) exec postgres psql -U qawave -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
	@make db-migrate
	@echo "$(GREEN)✅ Database reset complete$(NC)"

db-shell: ## Open PostgreSQL shell
	$(COMPOSE_CMD) exec postgres psql -U qawave -d qawave

db-backup: ## Backup database
	@mkdir -p backups
	$(COMPOSE_CMD) exec postgres pg_dump -U qawave qawave > backups/qawave-$$(date +%Y%m%d-%H%M%S).sql
	@echo "$(GREEN)✅ Backup created$(NC)"

# ==============================================================================
# Docker Commands
# ==============================================================================

docker-build: backend-docker frontend-docker ## Build all Docker images

docker-push: ## Push Docker images to registry
	@echo "$(BLUE)Pushing images...$(NC)"
	docker push qawave/backend:local
	docker push qawave/frontend:local

# ==============================================================================
# Health & Monitoring
# ==============================================================================

health: health-check-local ## Alias for health-check-local

health-check-local: ## Check health of local development services
	@./scripts/health-check-local.sh

health-check-local-quiet: ## Check local services health (quiet mode)
	@./scripts/health-check-local.sh --quiet

health-check-json: ## Check local services health (JSON output for CI)
	@./scripts/health-check-local.sh --json

health-check-staging: ## Run health checks on staging environment
	@./scripts/health-check.sh

watch-logs: ## Watch logs from specific service (usage: make watch-logs SVC=backend)
	$(COMPOSE_CMD) logs -f $(SVC)

# ==============================================================================
# Utilities
# ==============================================================================

shell-postgres: db-shell ## Alias for db-shell

shell-redis: ## Open Redis CLI
	$(COMPOSE_CMD) exec redis redis-cli

shell-kafka: ## Open Kafka shell
	$(COMPOSE_CMD) exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test --from-beginning

kafka-topics: ## List Kafka topics
	$(COMPOSE_CMD) exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

kafka-create-topic: ## Create Kafka topic (usage: make kafka-create-topic TOPIC=my-topic)
	$(COMPOSE_CMD) exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic $(TOPIC) --partitions 3 --replication-factor 1

# ==============================================================================
# CI/CD Helpers
# ==============================================================================

ci-test: ## Run all CI tests
	@make backend-lint
	@make backend-test
	@make frontend-lint
	@make frontend-typecheck
	@make frontend-test

ci-build: ## Run CI build
	@make backend-build
	@make frontend-build

format: backend-lint-fix frontend-lint-fix ## Format all code

# ==============================================================================
# Documentation
# ==============================================================================

docs-serve: ## Serve documentation locally
	@command -v mkdocs >/dev/null 2>&1 || pip install mkdocs-material
	mkdocs serve

docs-build: ## Build documentation
	mkdocs build
