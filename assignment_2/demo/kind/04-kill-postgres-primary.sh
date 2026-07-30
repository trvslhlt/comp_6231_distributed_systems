#!/usr/bin/env bash
# Scenario 4 (kind): kill the Postgres primary — automatic failover via Patroni.
# Unlike docker-compose (a hand-rolled primary + 2 static replicas with only manual
# pg_promote() recovery — see ../compose/05-kill-postgres-primary.sh), Postgres here is a
# symmetric 3-node StatefulSet managed by Patroni (db/patroni/), coordinating leader election
# over the same etcd cluster used elsewhere in this project. A graceful pod delete sends
# SIGTERM, which Patroni catches to proactively release its etcd leader lock *before* the pod
# terminates — so failover here is sub-second, not gated on a lease TTL. Whichever replica
# notices first promotes itself, patches its own pod's role label (on_role_change.py, same
# pattern as common/k8s/PodRoleLabeler for the load balancer), and the postgres-primary Service
# re-points there automatically. No pg_promote() call anywhere in this script.
# Assumes port-forward.sh is running in another terminal.
set -euo pipefail

OLD_PRIMARY=$(kubectl -n vehicle-rental get pods -l app=postgres,role=primary -o jsonpath='{.items[0].metadata.name}')
echo "--- current primary: $OLD_PRIMARY ---"
kubectl -n vehicle-rental get pods -l app=postgres --show-labels

echo "--- deleting $OLD_PRIMARY (graceful — SIGTERM lets Patroni release its lock immediately) ---"
kubectl -n vehicle-rental delete pod "$OLD_PRIMARY"

echo "--- /total keeps working throughout — Service B was already reading from a replica ---"
curl -s "http://localhost:8090/total?vehicleType=SUV&season=Summer&days=2"
echo

echo "--- waiting for a NEW primary to be elected (should be near-instant, not TTL-gated) ---"
until kubectl -n vehicle-rental get pods -l app=postgres,role=primary -o jsonpath='{.items[0].metadata.name}' 2>/dev/null \
    | grep -qv "^$OLD_PRIMARY$\|^$"; do
  sleep 1
done
NEW_PRIMARY=$(kubectl -n vehicle-rental get pods -l app=postgres,role=primary -o jsonpath='{.items[0].metadata.name}')
echo "--- new primary: $NEW_PRIMARY (was: $OLD_PRIMARY) ---"
kubectl -n vehicle-rental get pods -l app=postgres --show-labels

echo "--- Patroni's own timeline history confirms a real promotion, not a stale relabel ---"
# The pod label flips (and this script notices) slightly before Patroni finishes writing the
# new entry to its history file, so poll briefly rather than querying it exactly once.
for i in 1 2 3 4 5; do
  HISTORY=$(kubectl -n vehicle-rental exec "$NEW_PRIMARY" -- python3 -c \
    "import urllib.request, json; print(json.dumps(json.loads(urllib.request.urlopen('http://localhost:8008/history').read()), indent=2))")
  echo "$HISTORY" | grep -q "\"$NEW_PRIMARY\"" && break
  sleep 1
done
echo "$HISTORY"

echo "--- write capability is back with zero manual intervention ---"
kubectl -n vehicle-rental run psql-failover-check --image=postgres:16 --restart=Never --command \
  -- psql "postgresql://vehicle_rental:vehicle_rental@postgres-primary.vehicle-rental.svc.cluster.local:5432/vehicle_rental" \
     -c "CREATE TABLE failover_check (id serial primary key); DROP TABLE failover_check;"
kubectl -n vehicle-rental wait --for=jsonpath='{.status.phase}'=Succeeded pod/psql-failover-check --timeout=30s
kubectl -n vehicle-rental logs psql-failover-check
kubectl -n vehicle-rental delete pod psql-failover-check --ignore-not-found >/dev/null
