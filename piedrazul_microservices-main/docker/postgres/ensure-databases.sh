#!/bin/sh
set -eu

PGHOST="${PGHOST:-postgres}"
PGUSER="${POSTGRES_USER:-piedrazul}"

for db in piedrazul_usuarios piedrazul_personas piedrazul_citas piedrazul_notifications keycloak; do
  exists=$(psql -h "$PGHOST" -U "$PGUSER" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '${db}'")
  if [ "$exists" != "1" ]; then
    echo "Creando base de datos ${db}..."
    psql -h "$PGHOST" -U "$PGUSER" -d postgres -c "CREATE DATABASE ${db} OWNER ${PGUSER};"
  fi
done

echo "Bases de datos verificadas."
