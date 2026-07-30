#!/usr/bin/env bash
# Scenario 2 (kind): remove a Service B instance — service discovery + fault detection.
#
# Unlike docker-compose's version of this scenario (which uses `docker compose kill` to
# simulate a true crash and show lease-expiry-based removal), this scales the Deployment down
# by one instead. Two things make a "true crash" surprisingly hard to reproduce faithfully
# here:
#   - Kubernetes pod termination is graceful by design: `kubectl delete pod`, even with
#     `--grace-period=0 --force`, still sends SIGTERM, which is enough time for Spring's
#     shutdown hook to run EtcdServiceRegistry.close() and explicitly delete the etcd key —
#     so it shows immediate deregistration, not the lease-expiry path.
#   - Bypassing that entirely requires signaling the container from outside its own PID
#     namespace (SIGKILL sent *from within* a container to its own PID 1 is silently ignored
#     by the kernel — an undocumented-until-you-hit-it protection against accidentally killing
#     the container's init process). That's only reachable via the node's container runtime
#     directly, which is kind-specific and not something a real cluster gives you. And even
#     then, kubelet restarts the container in place fast enough that the app often re-registers
#     under the same key before the old lease would have expired, masking the gap.
#
# So: this scenario demonstrates the *deregistration* side of service discovery (an instance
# leaving cleanly), which is just as real a distributed-systems property. For the lease-expiry
# fault-detection path specifically, see ../compose/03-kill-service-b.sh.
set -euo pipefail

echo "--- current Service B pods ---"
kubectl -n vehicle-rental get pods -l app=service-vehicle-season-price

echo "--- scaling service-vehicle-season-price down to 2 replicas ---"
kubectl -n vehicle-rental scale deployment/service-vehicle-season-price --replicas=2
sleep 5

echo "--- etcd registration now shows only the 2 survivors ---"
kubectl -n vehicle-rental exec etcd-0 -- etcdctl get /services/service-vehicle-season-price/ --prefix

echo "--- confirming /total still works ---"
for i in 1 2 3; do
  curl -s "http://localhost:8090/total?vehicleType=SUV&season=Summer&days=2" \
    | python3 -c "import json,sys; print(json.load(sys.stdin)['debug']['upstreamInstanceId'])"
done

echo "--- scaling back to 3 replicas ---"
kubectl -n vehicle-rental scale deployment/service-vehicle-season-price --replicas=3
kubectl -n vehicle-rental wait --for=condition=Ready pods -l app=service-vehicle-season-price --timeout=60s
