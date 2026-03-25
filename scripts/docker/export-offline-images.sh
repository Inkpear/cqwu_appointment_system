#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

MODE="${1:-app-only}"
ENV_FILE="${2:-.env.prod}"
OUTPUT_DIR="${3:-offline_bundle_${MODE}_$(date +%Y%m%d_%H%M%S)}"

if [ "$MODE" != "app-only" ] && [ "$MODE" != "full" ]; then
  echo "Usage: bash scripts/docker/export-offline-images.sh [app-only|full] [env-file] [output-dir]"
  exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
  if [ -f ".env.example" ]; then
    ENV_FILE=".env.example"
    echo "Env file not found, fallback to .env.example"
  else
    echo "Env file not found: $ENV_FILE"
    exit 1
  fi
fi

set -a
# shellcheck source=/dev/null
. "$ENV_FILE"
set +a

APP_IMAGE_NAME="${APP_IMAGE_NAME:-cqwu-appointment-system}"
APP_IMAGE_TAG="${APP_IMAGE_TAG:-latest}"
APP_IMAGE="${APP_IMAGE_NAME}:${APP_IMAGE_TAG}"
FRONTEND_IMAGE_NAME="${FRONTEND_IMAGE_NAME:-cqwu-appointment-frontend}"
FRONTEND_IMAGE_TAG="${FRONTEND_IMAGE_TAG:-latest}"
FRONTEND_IMAGE="${FRONTEND_IMAGE_NAME}:${FRONTEND_IMAGE_TAG}"
FRONTEND_REPO_PATH="${FRONTEND_REPO_PATH:-./cqwu_appointment_system_vue}"
MYSQL_IMAGE="${MYSQL_IMAGE:-mysql:8.0}"
REDIS_IMAGE="${REDIS_IMAGE:-redis:7-alpine}"
MINIO_IMAGE="${MINIO_IMAGE:-minio/minio:latest}"
MINIO_MC_IMAGE="${MINIO_MC_IMAGE:-minio/mc:latest}"

if [ ! -d "$FRONTEND_REPO_PATH" ]; then
  echo "Frontend repo path not found: $FRONTEND_REPO_PATH"
  exit 1
fi

if [ ! -f "$FRONTEND_REPO_PATH/Dockerfile" ]; then
  echo "Frontend Dockerfile not found: $FRONTEND_REPO_PATH/Dockerfile"
  exit 1
fi

mkdir -p "$OUTPUT_DIR"

echo "[mode] $MODE"
echo "[1/6] Package Spring Boot jar"
./mvnw -DskipTests package

echo "[2/6] Build backend image: $APP_IMAGE"
docker compose --env-file "$ENV_FILE" build app

echo "[3/6] Build frontend image: $FRONTEND_IMAGE"
docker build -t "$FRONTEND_IMAGE" "$FRONTEND_REPO_PATH"

if [ "$MODE" = "full" ]; then
  echo "[4/6] Pull infrastructure images"
  docker pull "$MYSQL_IMAGE"
  docker pull "$REDIS_IMAGE"
  docker pull "$MINIO_IMAGE"
  docker pull "$MINIO_MC_IMAGE"
else
  echo "[4/6] Skip infrastructure images in app-only mode"
fi

echo "[5/6] Export images to tar"
if [ "$MODE" = "full" ]; then
  docker save -o "$OUTPUT_DIR/images.tar" \
    "$APP_IMAGE" \
    "$FRONTEND_IMAGE" \
    "$MYSQL_IMAGE" \
    "$REDIS_IMAGE" \
    "$MINIO_IMAGE" \
    "$MINIO_MC_IMAGE"
else
  docker save -o "$OUTPUT_DIR/images.tar" \
    "$APP_IMAGE" \
    "$FRONTEND_IMAGE"
fi

echo "[6/6] Copy deployment assets"
cp docker-compose.yml "$OUTPUT_DIR/"
cp .env.example "$OUTPUT_DIR/"
cp -r sql "$OUTPUT_DIR/"
cp scripts/docker/apply-offline-on-vm.sh "$OUTPUT_DIR/"

cat > "$OUTPUT_DIR/README_OFFLINE_DEPLOY.txt" <<EOF
1) Copy this folder to the VM.
2) On VM, create .env.prod from .env.example and fill secrets.
3) Run: bash apply-offline-on-vm.sh $MODE .env.prod images.tar
EOF

echo "Export complete: $OUTPUT_DIR"
