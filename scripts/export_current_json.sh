#!/usr/bin/env bash
set -euo pipefail

# Export full JSON from local app (admin endpoint) to backups
# Starts the app temporarily if it's not running.

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"
cd "$PROJECT_ROOT"

PORT=8585
EXPORT_URL="http://localhost:${PORT}/api/exports/getAll"
OUT_DIR="volumes/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUT_FILE="$OUT_DIR/exports_${TIMESTAMP}.json"

mkdir -p "$OUT_DIR"

was_running=false
if lsof -Pi :${PORT} -sTCP:LISTEN -t >/dev/null 2>&1 ; then
  was_running=true
else
  echo "Starting app to perform export..."
  ./mvnw spring-boot:run > /tmp/export_json.log 2>&1 &
  APP_PID=$!
  for i in $(seq 1 60); do
    if lsof -Pi :${PORT} -sTCP:LISTEN -t >/dev/null 2>&1 ; then
      break
    fi
    sleep 1
  done
fi

echo "Exporting JSON to $OUT_FILE ..."
curl -sf "$EXPORT_URL" -o "$OUT_FILE"
echo "✓ Export saved: $OUT_FILE"

if [ "$was_running" = false ]; then
  echo "Stopping temporary app..."
  kill $APP_PID >/dev/null 2>&1 || true
  wait $APP_PID >/dev/null 2>&1 || true
fi
