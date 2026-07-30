#!/usr/bin/env bash
# Scenario 4: kill the Postgres primary — read availability vs. write recovery.
# This assignment's traffic is 100% reads, so the key result is that /total keeps working
# after the primary goes down, since Service B was already reading from a replica.
set -euo pipefail
cd "$(dirname "$0")"

LB_PORT=$(./01-find-active-lb.sh)

echo "--- stopping postgres-primary ---"
docker compose stop postgres-primary

echo "--- /total still works — Service B reads from a replica, not the primary ---"
curl -s "http://localhost:$LB_PORT/total?vehicleType=SUV&season=Summer&days=2"
echo

echo "--- promoting replica-1 to demonstrate write-capability recovery ---"
docker exec assignment_2-postgres-replica-1-1 psql -U vehicle_rental -d vehicle_rental -c "SELECT pg_promote();"
docker exec assignment_2-postgres-replica-1-1 psql -U vehicle_rental -d vehicle_rental -c "SELECT pg_is_in_recovery();"
echo "(should print 'f' — replica-1 is now a writable primary)"

echo "--- restarting the old primary (it will NOT auto-rejoin as a replica of the newly-promoted node; that recovery is out of scope here) ---"
docker compose start postgres-primary
