#!/bin/bash

# db_migrate.sh
# Migrates database with optional remote data import
# Equivalent to Flask's db_migrate.py
#
# Usage:
# > scripts/db_migrate.sh              # Local reset only
# > scripts/db_migrate.sh --import     # Reset + import from remote

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"

# Change to project root
cd "$PROJECT_ROOT"

echo "=========================================="
echo "Database Migration Script"
echo "=========================================="
echo ""

# Check for --import flag
IMPORT_FLAG=""
if [ "$1" == "--import" ] || [ "$1" == "--import-remote" ]; then
    IMPORT_FLAG="--import-remote"
    echo "Mode: Migration with remote data import"
else
    echo "Mode: Local migration only"
    echo "(Use --import to fetch data from remote database)"
fi
echo ""

# Check if Spring Boot is running
if lsof -Pi :8585 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo "WARNING: Spring Boot application is running on port 8585"
    echo "Please stop it first: pkill -f 'spring-boot:run'"
    echo ""
    exit 1
fi

# Compile the DatabaseMigrator class
echo "Compiling DatabaseMigrator..."
./mvnw compile -q

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo ""
echo "Running database migration..."
echo ""

# Run using Maven exec plugin (has all dependencies in classpath)
./mvnw exec:java \
    -Dexec.mainClass="com.open.spring.system.DatabaseMigrator" \
    -Dexec.args="$IMPORT_FLAG" \
    -Dexec.cleanupDaemonThreads=false \
    -q

EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    echo ""
    echo "Database migration completed successfully!"
    echo ""
else
    echo ""
    echo "Database migration failed with exit code $EXIT_CODE"
    echo ""
fi

exit $EXIT_CODE