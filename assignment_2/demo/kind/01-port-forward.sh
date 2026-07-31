#!/usr/bin/env bash
# Forwards localhost:8090 to the load-balancer Service's port 80
# Run this in a terminal and leave it running
#
# NOTE: if the load-balancer instance fails, port forwarding must be reestablished
set -euo pipefail
kubectl -n vehicle-rental port-forward svc/load-balancer 8090:80
