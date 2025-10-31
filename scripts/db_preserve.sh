#!/usr/bin/env bash
set -euo pipefail

# db_preserve.sh
# Preserve and restore selected data (User/Groups/Bathroom) across upgrades.
# Steps:
# 1) Export JSON (admin endpoint)
# 2) Reset DB schema and load defaults (db_init.sh)
# 3) Re-import selected tables from the latest export

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"
cd "$PROJECT_ROOT"

# 1) Export JSON to backups
scripts/export_current_json.sh

# Find the latest export file
LATEST_JSON=$(ls -t volumes/backups/exports_*.json | head -n 1)
if [ -z "${LATEST_JSON:-}" ]; then
  echo "No export JSON found in volumes/backups"
  exit 1
fi

echo "Latest export: $LATEST_JSON"

# 2) Reset DB (fresh schema + defaults)
scripts/db_init.sh <<<'y'

# 3) Import selected tables using allowlist
# Default allowlist: person,groups,tinkle (override IMPORT_TABLES)
: "${IMPORT_TABLES:=person,groups,tinkle}"

echo "Re-importing tables: $IMPORT_TABLES"
IMPORT_TABLES="$IMPORT_TABLES" python3 scripts/import_json_to_sqlite.py "$LATEST_JSON" volumes/sqlite.db

echo "\n✓ Preserve workflow complete"
