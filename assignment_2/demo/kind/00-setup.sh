#!/usr/bin/env bash
# Brings up a local kind cluster and deploys the full stack — the Kubernetes equivalent of
# `docker compose up`. Run once before any of the numbered scenario scripts below.
set -euo pipefail
cd "$(dirname "$0")/../.."   # assignment_2/, so relative Dockerfile and k8s/ paths resolve

kind create cluster --name vehicle-rental

docker build -t vehicle-rental/service-vehicle-season-price:latest -f service-vehicle-season-price/Dockerfile .
docker build -t vehicle-rental/service-vehicle-total-price:latest -f service-vehicle-total-price/Dockerfile .
docker build -t vehicle-rental/load-balancer:latest -f load-balancer/Dockerfile .

# Copies the three locally-built images into kind's node — without this, kubelet would try
# (and fail) to pull them from a real registry, since they only exist on your machine.
kind load docker-image \
  vehicle-rental/service-vehicle-season-price:latest \
  vehicle-rental/service-vehicle-total-price:latest \
  vehicle-rental/load-balancer:latest \
  --name vehicle-rental

kubectl apply -f k8s/

echo "Waiting for every pod to reach Ready (postgres-replica pods take longest — each runs"
echo "pg_basebackup against the primary before Postgres itself starts)..."
kubectl -n vehicle-rental wait --for=condition=Ready pods --all --timeout=300s

echo "All pods Ready. Next: run ./port-forward.sh in its own terminal, then the numbered scripts."
