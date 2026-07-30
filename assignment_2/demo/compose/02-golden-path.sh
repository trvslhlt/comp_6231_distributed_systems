#!/usr/bin/env bash
# Scenario 1: golden path + load balancing across N instances.
# Calls /total four times through the active load balancer and prints which Service A
# instance served each request and which Service B instance it called upstream — both should
# alternate, since each hop is independently load-balanced via etcd.
set -euo pipefail
cd "$(dirname "$0")"

LB_PORT=$(./01-find-active-lb.sh)
echo "Active load balancer: localhost:$LB_PORT"

for i in 1 2 3 4; do
  curl -s "http://localhost:$LB_PORT/total?vehicleType=SUV&season=Summer&days=2" \
    | python3 -c "import json,sys; d=json.load(sys.stdin)['debug']; print('servedBy:', d['servedByInstanceId'], '| upstream:', d['upstreamInstanceId'])"
done
