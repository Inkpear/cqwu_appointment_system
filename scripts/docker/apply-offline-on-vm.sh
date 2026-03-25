#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-app-only}"
ENV_FILE="${2:-.env.prod}"
IMAGES_TAR="${3:-images.tar}"

resolve_env() {
  local key="$1"
  local default_value="$2"
  local value
  value=$(grep -E "^${key}=" "$ENV_FILE" | tail -n1 | cut -d'=' -f2- || true)
  if [ -z "${value:-}" ]; then
    echo "$default_value"
  else
    echo "$value"
  fi
}

bootstrap_schema_if_needed() {
  local db_host="$1"
  local db_name="$2"

  if [ "$db_host" != "mysql" ]; then
    echo "Skip schema bootstrap: DB_HOST=$db_host (not compose mysql service)."
    return
  fi

  local mysql_container_id
  mysql_container_id=$(docker compose --env-file "$ENV_FILE" ps -q mysql || true)
  if [ -z "${mysql_container_id:-}" ]; then
    echo "ERROR: app-only mode uses DB_HOST=mysql, but mysql service is not running."
    echo "Run full mode once, or start mysql service before app-only deployment."
    exit 1
  fi

  local table_count
  table_count=$(docker compose --env-file "$ENV_FILE" exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = \"'"$db_name"'\";"' || true)
  table_count=$(echo "${table_count:-}" | tr -d '[:space:]')

  if [ -z "$table_count" ] || [ "$table_count" = "0" ]; then
    echo "Detected empty schema ${db_name}, applying sql/init.sql ..."
    docker compose --env-file "$ENV_FILE" exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < sql/init.sql
    echo "Database bootstrap applied."
  else
    echo "Schema ${db_name} already has $table_count table(s), skip bootstrap SQL."
  fi
}

if [ "$MODE" != "app-only" ] && [ "$MODE" != "full" ]; then
  echo "Usage: bash apply-offline-on-vm.sh [app-only|full] [env-file] [images-tar]"
  exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing env file: $ENV_FILE"
  exit 1
fi

if [ ! -f "$IMAGES_TAR" ]; then
  echo "Missing images tar: $IMAGES_TAR"
  exit 1
fi

DB_HOST=$(resolve_env "DB_HOST" "mysql")
DB_NAME=$(resolve_env "DB_NAME" "cqwu")
MYSQL_DATABASE=$(resolve_env "MYSQL_DATABASE" "$DB_NAME")

echo "[1/3] Load local images"
docker load -i "$IMAGES_TAR"

echo "[2/3] Start services with local images only (mode: $MODE)"
if [ "$MODE" = "full" ]; then
  docker compose --env-file "$ENV_FILE" up -d --no-build

  echo "[2.1/3] Check database schema bootstrap status"
  TABLE_COUNT=$(docker compose --env-file "$ENV_FILE" exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = \"${MYSQL_DATABASE}\";"')
  TABLE_COUNT=$(echo "$TABLE_COUNT" | tr -d '[:space:]')

  if [ -z "$TABLE_COUNT" ] || [ "$TABLE_COUNT" = "0" ]; then
    echo "Detected empty schema ${MYSQL_DATABASE:-cqwu}, applying sql/init.sql ..."
    docker compose --env-file "$ENV_FILE" exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < sql/init.sql
    echo "Database bootstrap applied."
  else
    echo "Schema ${MYSQL_DATABASE:-cqwu} already has $TABLE_COUNT table(s), skip bootstrap SQL."
  fi
else
  bootstrap_schema_if_needed "$DB_HOST" "$MYSQL_DATABASE"
  docker compose --env-file "$ENV_FILE" up -d --no-build --no-deps app frontend
fi

echo "[3/3] Show service status"
docker compose --env-file "$ENV_FILE" ps
