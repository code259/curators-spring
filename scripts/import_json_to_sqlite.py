#!/usr/bin/env python3
"""
Import data from a JSON export into the local SQLite database.

Features:
- Allowlist specific tables via IMPORT_TABLES env var (default: person,groups,tinkle)
- Intersect JSON columns with actual table columns (PRAGMA-based)
- JSON-serialize dict/list values to fit TEXT/JSON columns
- INSERT OR REPLACE semantics
"""

import json
import sqlite3
import sys
import os
from pathlib import Path

def _get_existing_columns(cursor, table_name: str):
    cursor.execute(f"PRAGMA table_info(\"{table_name}\")")
    return {row[1] for row in cursor.fetchall()}  # column name at index 1


def _coerce_value(v):
    # Convert unsupported Python types (dict/list) to JSON strings for SQLite TEXT/JSON columns
    if isinstance(v, (dict, list)):
        return json.dumps(v, ensure_ascii=False)
    return v


def import_data_to_sqlite(json_file='volumes/data.json', db_file='volumes/sqlite.db'):
    """Import data from JSON file into SQLite database (allowlist + safe inserts)."""
    
    print("=" * 60)
    print("Importing Data to SQLite")
    print("=" * 60)
    print()
    
    # Check if files exist
    if not Path(json_file).exists():
        print(f"Error: JSON file not found: {json_file}")
        return False
    
    if not Path(db_file).exists():
        print(f"Error: Database file not found: {db_file}")
        return False
    
    # Load JSON data
    print(f"Reading data from: {json_file}")
    try:
        with open(json_file, 'r') as f:
            data = json.load(f)
        print(f"Found {len(data)} tables in JSON file")
    except Exception as e:
        print(f"Error reading JSON: {e}")
        return False
    
    print()
    
    # Connect to database
    print(f"Connecting to database: {db_file}")
    try:
        conn = sqlite3.connect(db_file)
        cursor = conn.cursor()
        print(f"Connected successfully")
    except Exception as e:
        print(f"Error connecting to database: {e}")
        return False
    
    print()
    
    # Get existing tables in database
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")
    existing_tables = {row[0] for row in cursor.fetchall()}
    print(f"Existing tables in database: {len(existing_tables)}")

    # Determine allowlist behavior
    # By default: import ALL tables
    # If IMPORT_TABLES is set to a comma list, filter to those tables.
    # If IMPORT_TABLES is '*' or 'ALL' (case-insensitive), import ALL tables.
    env_allow = os.getenv("IMPORT_TABLES")
    allowlist = None  # None means import all tables
    if env_allow is not None:
        token = env_allow.strip()
        if token and token.upper() not in {"*", "ALL"}:
            allowlist = {t.strip() for t in token.split(',') if t.strip()}
    if allowlist is None:
        print("Allowlist: ALL tables")
    else:
        print(f"Allowlist: {', '.join(sorted(allowlist))}")
    
    print()
    print("Importing data...")
    print()
    
    # Import data table by table
    imported_count = 0
    skipped_count = 0
    error_count = 0
    
    for table_name, records in data.items():
        # Only import tables in allowlist if provided (case-sensitive match to actual tables)
        if allowlist is not None and table_name not in allowlist:
            continue
        # Skip empty tables
        if not records or len(records) == 0:
            continue
        
        # Check if table exists
        if table_name not in existing_tables:
            print(f"  Skipping {table_name}: table doesn't exist in schema")
            skipped_count += 1
            continue
        
        try:
            # Intersect JSON keys with actual table columns
            table_columns = _get_existing_columns(cursor, table_name)
            if not table_columns:
                print(f"  Skipping {table_name}: no columns discovered via PRAGMA")
                skipped_count += 1
                continue

            columns = [col for col in records[0].keys() if col in table_columns]
            if not columns:
                print(f"  Skipping {table_name}: no matching columns between JSON and DB")
                skipped_count += 1
                continue
            
            # Prepare insert statement
            placeholders = ','.join(['?' for _ in columns])
            column_names = ','.join([f'"{col}"' for col in columns])
            insert_sql = f'INSERT OR REPLACE INTO "{table_name}" ({column_names}) VALUES ({placeholders})'
            
            # Insert all records
            batch = []
            for record in records:
                values = [_coerce_value(record.get(col)) for col in columns]
                batch.append(values)
            cursor.executemany(insert_sql, batch)
            
            conn.commit()
            print(f"  {table_name}: {len(records)} records imported")
            imported_count += 1
            
        except Exception as e:
            print(f"  {table_name}: Error - {str(e)[:120]}")
            error_count += 1
            conn.rollback()
    
    conn.close()
    
    print()
    print("=" * 60)
    print("Import Summary")
    print("=" * 60)
    print(f"  Imported: {imported_count} tables")
    print(f"  Skipped: {skipped_count} tables (not in schema/allowlist)")
    print(f"  Errors: {error_count} tables")
    print()
    
    if imported_count > 0:
        print("Import completed successfully!")
        return True
    else:
        print("No data was imported")
        return False

if __name__ == "__main__":
    json_file = sys.argv[1] if len(sys.argv) > 1 else 'volumes/data.json'
    db_file = sys.argv[2] if len(sys.argv) > 2 else 'volumes/sqlite.db'
    
    success = import_data_to_sqlite(json_file, db_file)
    sys.exit(0 if success else 1)