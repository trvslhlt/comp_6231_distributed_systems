#!/usr/bin/env bash
# Scenario 3: kill the active load balancer — leader election failover.
# Stops whichever load-balancer replica currently holds leadership, waits past the etcd
# lease TTL, and confirms the other replica has taken over and now answers 200.
set -euo pipefail
cd "$(dirname "$0")"

LB_PORT=$(./01-find-active-lb.sh)
if [ "$LB_PORT" = "8090" ]; then
  ACTIVE_SERVICE=load-balancer-1
else
  ACTIVE_SERVICE=load-balancer-2
fi

echo "--- stopping the active instance: $ACTIVE_SERVICE (port $LB_PORT) ---"
docker compose stop "$ACTIVE_SERVICE"

echo "--- waiting 8s (ETCD_LEASE_TTL_SECONDS=10 in compose) ---"
sleep 8

echo "--- checking both ports again — the survivor should now answer 200; the port we just"
echo "    stopped is expected to refuse the connection entirely (curl exit 7, shown as 000) ---"
curl -s -o /dev/null -w "8090 -> %{http_code}\n" "http://localhost:8090/total?vehicleType=SUV&season=Summer&days=2" || true
curl -s -o /dev/null -w "8091 -> %{http_code}\n" "http://localhost:8091/total?vehicleType=SUV&season=Summer&days=2" || true

echo "--- restoring $ACTIVE_SERVICE ---"
docker compose start "$ACTIVE_SERVICE"
