#!/usr/bin/env bash
# Brings up a local kind cluster and deploys the full stack — the Kubernetes equivalent of
# `docker compose up`. Run once before any of the numbered scenario scripts below.
set -euo pipefail
cd "$(dirname "$0")/../.."   # assignment_2/, so relative Dockerfile and k8s/ paths resolve

kind create cluster --name vehicle-rental

docker build -t vehicle-rental/postgres-patroni:latest ./db/patroni
docker build -t vehicle-rental/service-vehicle-season-price:latest -f service-vehicle-season-price/Dockerfile .
docker build -t vehicle-rental/service-vehicle-total-price:latest -f service-vehicle-total-price/Dockerfile .
docker build -t vehicle-rental/load-balancer:latest -f load-balancer/Dockerfile .

# Copies the four locally-built images into kind's node — without this, kubelet would try
# (and fail) to pull them from a real registry, since they only exist on your machine.
kind load docker-image \
  vehicle-rental/postgres-patroni:latest \
  vehicle-rental/service-vehicle-season-price:latest \
  vehicle-rental/service-vehicle-total-price:latest \
  vehicle-rental/load-balancer:latest \
  --name vehicle-rental

kubectl apply -f k8s/

echo "Waiting for the postgres StatefulSet's 3 replicas (each clones the current primary via"
echo "Patroni's own bootstrap before Postgres starts). \`kubectl wait --for=condition=Ready\`"
echo "only snapshots pods that exist when it's invoked, which OrderedReady pod creation can"
echo "race — \`rollout status\` is replica-count-aware instead, so it actually waits for all 3."
kubectl -n vehicle-rental rollout status statefulset/postgres --timeout=300s

echo "Waiting for every remaining pod to reach Ready (Service A/B pods may restart a few times"
echo "first, since nothing gates their startup on Postgres being ready yet)..."
kubectl -n vehicle-rental wait --for=condition=Ready pods --all --timeout=300s

echo "All pods Ready. Next: run ./01-port-forward.sh in its own terminal, then the numbered scripts."
