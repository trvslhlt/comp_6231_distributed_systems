#!/usr/bin/env bash
# Remove a Service B instance 
# — service discovery
# - fault detection
set -euo pipefail

echo "--- current Service B pods (instances) ---"
kubectl -n vehicle-rental get pods -l app=service-vehicle-season-price
echo
echo "--- scale down to 2 replicas ---"
kubectl -n vehicle-rental scale deployment/service-vehicle-season-price --replicas=2
sleep 5
echo
echo "--- etcd registration shows 2 remaining instances ---"
kubectl -n vehicle-rental exec etcd-0 -- etcdctl get /services/service-vehicle-season-price/ --prefix
echo
echo "--- confirming application still works ---"
for i in 1 2 3; do
  curl -s "http://localhost:8090/total?vehicleType=SUV&season=Summer&days=2" \
    | python3 -c "import json,sys; print(json.load(sys.stdin)['debug']['upstreamInstanceId'])"
done
echo
echo "--- scale back up to 3 instances ---"
kubectl -n vehicle-rental scale deployment/service-vehicle-season-price --replicas=3
kubectl -n vehicle-rental wait --for=condition=Ready pods -l app=service-vehicle-season-price --timeout=60s
