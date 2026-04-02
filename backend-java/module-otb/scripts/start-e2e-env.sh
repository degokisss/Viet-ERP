#!/bin/bash
# E2E environment for module-otb: starts PostgreSQL + Redis, runs auth integration tests, tears down

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$MODULE_DIR/docker-compose.yml"

echo "=== Starting e2e environment for module-otb ==="

# Start docker-compose
echo "Starting PostgreSQL and Redis..."
docker compose -f "$COMPOSE_FILE" up -d

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL..."
for i in $(seq 1 30); do
  if docker compose -f "$COMPOSE_FILE" exec -T postgres pg_isready -U postgres > /dev/null 2>&1; then
    echo "PostgreSQL is ready."
    break
  fi
  if [ $i -eq 30 ]; then
    echo "ERROR: PostgreSQL did not become ready in time."
    docker compose -f "$COMPOSE_FILE" down
    exit 1
  fi
  sleep 1
done

# Wait for Redis to be ready
echo "Waiting for Redis..."
for i in $(seq 1 10); do
  if docker compose -f "$COMPOSE_FILE" exec -T redis redis-cli ping > /dev/null 2>&1; then
    echo "Redis is ready."
    break
  fi
  if [ $i -eq 10 ]; then
    echo "WARNING: Redis did not respond, continuing anyway..."
  fi
  sleep 1
done

# Run integration tests
echo "Running auth integration tests..."
cd "$MODULE_DIR"
mvn test -pl module-otb -Dtest=AuthServiceIntegrationTest,BudgetServiceIntegrationTest \
  -Dspring.profiles.active=dev \
  -q

TEST_RESULT=$?

# Tear down
echo "Tearing down..."
docker compose -f "$COMPOSE_FILE" down

if [ $TEST_RESULT -ne 0 ]; then
  echo "E2E tests FAILED."
  exit 1
fi

echo "=== E2E environment complete ==="
