#!/bin/sh
# Runs exactly once, only on the node that bootstraps a brand new cluster (never on nodes that
# join as replicas). $1 is a ready-to-use libpq connection string to the freshly-initialized
# instance; Patroni already set up passwordless auth for it via PGPASSFILE, so plain psql just
# works. Creates the application role/database this project's services actually connect as —
# equivalent to what POSTGRES_DB/POSTGRES_USER/POSTGRES_PASSWORD do in the vanilla postgres
# image's docker-entrypoint.sh, which Patroni bypasses entirely.
set -e

psql "$1" -v ON_ERROR_STOP=1 <<-EOSQL
    CREATE ROLE ${APP_DB_USER} WITH LOGIN PASSWORD '${APP_DB_PASSWORD}';
    CREATE DATABASE ${APP_DB_NAME} OWNER ${APP_DB_USER};
EOSQL
