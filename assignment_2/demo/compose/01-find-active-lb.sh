#!/usr/bin/env bash
# Prints whichever load-balancer port (8090 or 8091) currently answers 200 for /total — that's
# the etcd election winner. The other port belongs to the passive instance and answers 503.
# Usage: LB_PORT=$(./01-find-active-lb.sh)
set -euo pipefail

for port in 8090 8091; do
  code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/total?vehicleType=SUV&season=Summer&days=2")
  if [ "$code" = "200" ]; then
    echo "$port"
    exit 0
  fi
done

echo "No load-balancer instance answered 200 on 8090 or 8091 — is the stack up? (docker compose ps)" >&2
exit 1
