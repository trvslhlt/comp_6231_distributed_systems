#!/usr/bin/env bash
# Scenario 2: kill a Service B instance — service discovery + fault detection.
# Uses `docker compose kill` (SIGKILL, no grace period) rather than `stop` (SIGTERM):
# `stop` lets Spring's shutdown hook run EtcdServiceRegistry.close(), which explicitly
# deletes the etcd key immediately — that's graceful deregistration, not fault detection.
# `kill` simulates a real crash, so the key only disappears once its lease naturally expires,
# which is the mechanism this scenario is actually meant to demonstrate.
set -euo pipefail
cd "$(dirname "$0")"

LB_PORT=$(./01-find-active-lb.sh)

echo "--- killing service-vehicle-season-price-1 (SIGKILL, simulates a crash) ---"
docker compose kill service-vehicle-season-price-1

echo "--- etcd registration immediately after stopping (lease not expired yet) ---"
docker exec assignment_2-etcd-1 etcdctl get /services/service-vehicle-season-price/ --prefix

echo "--- waiting 12s for the lease to expire ---"
sleep 12

echo "--- etcd registration after the lease expired (should show only season-price-2) ---"
docker exec assignment_2-etcd-1 etcdctl get /services/service-vehicle-season-price/ --prefix

echo "--- confirming every request now goes to season-price-2 ---"
for i in 1 2 3; do
  curl -s "http://localhost:$LB_PORT/total?vehicleType=SUV&season=Summer&days=2" \
    | python3 -c "import json,sys; print(json.load(sys.stdin)['debug']['upstreamInstanceId'])"
done

echo "--- restoring service-vehicle-season-price-1 ---"
docker compose start service-vehicle-season-price-1
