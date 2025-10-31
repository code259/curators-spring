#!/bin/bash


# db_init.sh
# Resets the database to original state with default data
# Just like Flask's db_migrate.py
#
# Usage:
# > scripts/db_init.sh
# > FORCE_YES=true scripts/db_init.sh


# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/.." && pwd )"


# Change to project root
cd "$PROJECT_ROOT"


echo "=========================================="
echo "Database Reset to Original State"
echo "=========================================="
echo ""


# Check if Spring Boot is running
if lsof -Pi :8585 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo "  WARNING: Spring Boot application is running on port 8585"
    echo "  Please stop it first: pkill -f 'spring-boot:run'"
    echo ""
    exit 1
fi


# Check for confirmation unless FORCE_YES is set
if [ "$FORCE_YES" != "true" ]; then
    echo "  This will reset the database to original state with default data"
    echo "  All current data will be lost (after backup)"
    echo ""
    read -p "Continue? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Cancelled."
        exit 0
    fi
fi


# Backup current database
if [ -f "volumes/sqlite.db" ]; then
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    mkdir -p volumes/backups
    cp volumes/sqlite.db "volumes/backups/sqlite_backup_${TIMESTAMP}.db"
    echo " Database backed up to: volumes/backups/sqlite_backup_${TIMESTAMP}.db"
   
    # Backup WAL files if they exist
    if [ -f "volumes/sqlite.db-wal" ]; then
        cp volumes/sqlite.db-wal "volumes/backups/sqlite_backup_${TIMESTAMP}.db-wal"
        echo " WAL file backed up"
    fi
    if [ -f "volumes/sqlite.db-shm" ]; then
        cp volumes/sqlite.db-shm "volumes/backups/sqlite_backup_${TIMESTAMP}.db-shm"
        echo " SHM file backed up"
    fi
fi


# Remove old database files
echo ""
echo "Removing old database..."
rm -f volumes/sqlite.db volumes/sqlite.db-wal volumes/sqlite.db-shm
echo " Old database removed"


# Temporarily change ddl-auto to create for this run
echo ""
echo "Starting Spring Boot to recreate schema and load default data..."
echo "(This will take a few seconds...)"
echo ""


# Run Spring Boot with ddl-auto=create to recreate tables and trigger ModelInit
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.jpa.hibernate.ddl-auto=create" > /tmp/db_init.log 2>&1 &
SPRING_PID=$!


# Wait for application to start
echo "Waiting for application to initialize..."
COUNTER=0
MAX_WAIT=60


while [ $COUNTER -lt $MAX_WAIT ]; do
    if lsof -Pi :8585 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
        echo " Application started"
        sleep 3  # Give ModelInit time to run
        break
    fi
    sleep 1
    COUNTER=$((COUNTER + 1))
   
    # Check if process died
    if ! kill -0 $SPRING_PID 2>/dev/null; then
        echo " Application failed to start. Check /tmp/db_init.log for errors"
        exit 1
    fi
done


if [ $COUNTER -eq $MAX_WAIT ]; then
    echo " Timeout waiting for application to start"
    kill $SPRING_PID 2>/dev/null
    exit 1
fi


# Stop the application
echo "Stopping application..."
kill $SPRING_PID 2>/dev/null
wait $SPRING_PID 2>/dev/null


# Give it time to shutdown cleanly
sleep 2


echo ""
echo "============================================================"
echo " DATABASE RESET COMPLETE"
echo "============================================================"
echo ""
echo "The database has been reset to original state with:"
echo "   Fresh schema created"
echo "   Default data loaded (Person.init(), QuizScore.init(), etc.)"
echo ""
echo "You can now start your application normally:"
echo "  ./mvnw spring-boot:run"
echo ""


