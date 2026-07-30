#!/usr/bin/env bash
# Scenario 4 (kind): kill the Postgres primary pod.
# The StatefulSet recreates it, reattached to the same PersistentVolumeClaim; reads keep
# working throughout via postgres-replica, same result as the compose scenario. Promotes
# replica-0 to demonstrate the write-capability recovery path.
# Assumes port-forward.sh is running in another terminal.
set -euo pipefail

echo "--- deleting postgres-primary-0 ---"
kubectl -n vehicle-rental delete pod postgres-primary-0

echo "--- /total still works via postgres-replica ---"
curl -s "http://localhost:8090/total?vehicleType=SUV&season=Summer&days=2"
echo

echo "--- promoting replica-0 to a writable primary ---"
kubectl -n vehicle-rental exec postgres-replica-0 -- \
  psql -U vehicle_rental -d vehicle_rental -c "SELECT pg_promote();"
