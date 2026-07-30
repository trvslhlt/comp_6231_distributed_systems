#!/usr/bin/env bash
# Forwards localhost:8090 to the client-facing load-balancer Service's port 80. kind has no
# cloud load-balancer provisioner, so this replaces the `type: LoadBalancer` external IP.
# Run this in its own terminal and leave it running — every other script here assumes it's up.
#
# NOTE: `kubectl port-forward svc/...` resolves to one specific backing pod when it starts and
# stays bound to that pod's network namespace — it does NOT dynamically follow the Service's
# endpoint list. If that pod is deleted (e.g. by 03-kill-load-balancer.sh below), the tunnel
# dies with "lost connection to pod" and this script must be re-run. Verified live: this is
# real kubectl behavior, not a fluke.
set -euo pipefail
kubectl -n vehicle-rental port-forward svc/load-balancer 8090:80
