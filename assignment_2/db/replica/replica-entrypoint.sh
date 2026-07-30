#!/bin/sh
# Custom entrypoint for a Postgres streaming-replication read replica. On first start 
# (empty PGDATA) it clones the primary via pg_basebackup with -R, which writes 
# standby.signal + primary_conninfo for us; the normal postgres entrypoint then starts 
# it in hot-standby mode. On restart, PGDATA is already populated, so this just hands 
# off to the normal entrypoint directly.
set -e

if [ -z "$(ls -A "$PGDATA" 2>/dev/null)" ]; then
    echo "replica: empty data directory, waiting for primary at ${PRIMARY_HOST}:5432..."
    until pg_isready -h "$PRIMARY_HOST" -p 5432 -U "$POSTGRES_REPLICATION_USER" >/dev/null 2>&1; do
        sleep 1
    done

    echo "replica: taking base backup from primary"
    PGPASSWORD="$POSTGRES_REPLICATION_PASSWORD" pg_basebackup \
        -h "$PRIMARY_HOST" -p 5432 -D "$PGDATA" -U "$POSTGRES_REPLICATION_USER" \
        -Fp -Xs -P -R
fi

exec docker-entrypoint.sh postgres
