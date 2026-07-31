#!/usr/bin/env bash
# kill DB primary
# — automatic failover via Patroni
# - 'role=primary' pod label update
set -euo pipefail

OLD_PRIMARY=$(kubectl -n vehicle-rental get pods -l app=postgres,role=primary -o jsonpath='{.items[0].metadata.name}')
echo "--- current primary: $OLD_PRIMARY ---"
kubectl -n vehicle-rental get pods -l app=postgres --show-labels
echo
echo "--- delete $OLD_PRIMARY ---"
kubectl -n vehicle-rental delete pod "$OLD_PRIMARY"
echo
echo "--- application still works ---"
curl -s "http://localhost:8090/total?vehicleType=SUV&season=Summer&days=2"
echo
echo
echo "--- wait for new primary to be elected ---"
until kubectl -n vehicle-rental get pods -l app=postgres,role=primary -o jsonpath='{.items[0].metadata.name}' 2>/dev/null \
    | grep -qv "^$OLD_PRIMARY$\|^$"; do
  sleep 1
done
NEW_PRIMARY=$(kubectl -n vehicle-rental get pods -l app=postgres,role=primary -o jsonpath='{.items[0].metadata.name}')
echo
echo "--- new primary: $NEW_PRIMARY (was: $OLD_PRIMARY) ---"
kubectl -n vehicle-rental get pods -l app=postgres --show-labels
echo
echo "--- Patroni's history confirms a real promotion ---"
# The pod label flips (and this script notices) slightly before Patroni finishes writing the
# new entry to its history file, so poll briefly rather than querying it exactly once. Checking
# specifically that the LAST entry's node is NEW_PRIMARY (not just that its name appears
# somewhere in the recent entries) matters on a long-lived cluster: after enough failovers,
# NEW_PRIMARY's name is likely already sitting in an older entry, which would make a substring
# match succeed immediately — before the actually-new entry for this promotion is written.
for i in 1 2 3 4 5; do
  LATEST_NODE=$(kubectl -n vehicle-rental exec "$NEW_PRIMARY" -- python3 -c \
    "import urllib.request, json; h=json.loads(urllib.request.urlopen('http://localhost:8008/history').read()); print(h[-1][4])")
  [ "$LATEST_NODE" = "$NEW_PRIMARY" ] && break
  sleep 1
done
kubectl -n vehicle-rental exec "$NEW_PRIMARY" -- python3 -c \
  "import urllib.request, json; h=json.loads(urllib.request.urlopen('http://localhost:8008/history').read()); print(json.dumps(h[-3:], indent=2))"
echo
echo "--- write capability is back ---"
kubectl -n vehicle-rental run psql-failover-check --image=postgres:16 --restart=Never --command \
  -- psql "postgresql://vehicle_rental:vehicle_rental@postgres-primary.vehicle-rental.svc.cluster.local:5432/vehicle_rental" \
     -c "CREATE TABLE failover_check (id serial primary key); DROP TABLE failover_check;"
kubectl -n vehicle-rental wait --for=jsonpath='{.status.phase}'=Succeeded pod/psql-failover-check --timeout=30s
kubectl -n vehicle-rental logs psql-failover-check
kubectl -n vehicle-rental delete pod psql-failover-check --ignore-not-found >/dev/null
