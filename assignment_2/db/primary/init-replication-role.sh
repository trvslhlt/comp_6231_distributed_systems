#!/bin/sh
# Runs once, on first startup of an empty data directory (standard postgres image behavior
# for anything mounted under /docker-entrypoint-initdb.d/). Creates the replication role the
# read replicas connect as, and opens pg_hba.conf to replication connections from the rest of
# the docker/k8s network.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE ROLE ${POSTGRES_REPLICATION_USER} WITH REPLICATION LOGIN PASSWORD '${POSTGRES_REPLICATION_PASSWORD}';
EOSQL

echo "host replication ${POSTGRES_REPLICATION_USER} all md5" >> "$PGDATA/pg_hba.conf"
