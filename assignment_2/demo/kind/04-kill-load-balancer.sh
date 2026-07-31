#!/usr/bin/env bash
# Scenario 3 (kind): kill the active load balancer — the role label flip, visible this time.
# Deletes whichever load-balancer pod is currently labeled role=active and watches the
# surviving pod win re-election and get relabeled — the client-facing Service's endpoint list
# updates the moment that happens, no client-side retry logic needed.
#
# NOTE: this deletes the exact pod 01-port-forward.sh is bound to, which breaks that tunnel (see
# the NOTE in 01-port-forward.sh) — re-run it afterward before continuing to the next scenario.
set -euo pipefail

echo "--- current load balancer labels ---"
kubectl -n vehicle-rental get pods -l app=load-balancer --show-labels
echo
echo "--- delete the active instance ---"
kubectl -n vehicle-rental get pods -l app=load-balancer,role=active -o name \
  | xargs kubectl -n vehicle-rental delete
echo
echo "--- watch role=active move to the surviving instance ---"
kubectl -n vehicle-rental get pods -l app=load-balancer --show-labels -w
