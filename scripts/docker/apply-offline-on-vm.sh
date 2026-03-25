#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-app-only}"
ENV_FILE="${2:-.env.prod}"
IMAGES_TAR="${3:-images.tar}"

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
  JPA_DDL_AUTO=none docker compose --env-file "$ENV_FILE" up -d --no-build --no-deps app frontend
fi

echo "[3/3] Show service status"
docker compose --env-file "$ENV_FILE" ps
