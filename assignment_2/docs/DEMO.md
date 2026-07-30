# Presentation Demo Script

Run against the docker-compose stack (`docker compose up --build -d` — see
[SETUP.md](SETUP.md)). All of this was exercised directly while building the system, not just
written down.

First, find the active load balancer (check both, the leader answers 200):

```bash
curl -s -o /dev/null -w "%{http_code}\n" "http://localhost:8090/total?vehicleType=SUV&season=Summer&days=2"
curl -s -o /dev/null -w "%{http_code}\n" "http://localhost:8091/total?vehicleType=SUV&season=Summer&days=2"
```

Use whichever port answered 200 as `$LB_PORT` below.

## 1. Golden path + load balancing across N instances

```bash
for i in 1 2 3 4; do
  curl -s "http://localhost:$LB_PORT/total?vehicleType=SUV&season=Summer&days=2" \
    | python3 -c "import json,sys; d=json.load(sys.stdin); print('servedBy:', d['servedByInstanceId'], '| upstream:', d['upstreamInstanceId'])"
done
```

`servedByInstanceId` (which Service A instance) and `upstreamInstanceId` (which Service B
instance) both alternate across calls — two independent load-balancing hops, both resolved
through etcd, both visible in one response.

## 2. Kill a Service B instance — service discovery + fault detection

```bash
docker compose stop service-vehicle-season-price-1
docker exec assignment_2-etcd-1 etcdctl get /services/service-vehicle-season-price/ --prefix
# still shows season-price-1 for a few seconds — its lease hasn't expired yet
sleep 12
docker exec assignment_2-etcd-1 etcdctl get /services/service-vehicle-season-price/ --prefix
# now only season-price-2

for i in 1 2 3; do
  curl -s "http://localhost:$LB_PORT/total?vehicleType=SUV&season=Summer&days=2" \
    | python3 -c "import json,sys; print(json.load(sys.stdin)['upstreamInstanceId'])"
done
# every response now comes from season-price-2

docker compose start service-vehicle-season-price-1   # restore it
```

## 3. Kill the active load balancer — leader election failover

```bash
docker compose stop load-balancer-2   # assuming lb-2 was active; use whichever port was 200 above
sleep 8                               # ETCD_LEASE_TTL_SECONDS=10 in compose
curl -s -o /dev/null -w "%{http_code}\n" "http://localhost:8090/total?vehicleType=SUV&season=Summer&days=2"
# the other instance now answers 200 — it won the re-election

docker compose start load-balancer-2
```

In Kubernetes this is more visible: `kubectl -n vehicle-rental get pods -l app=load-balancer
--show-labels` shows the `role` label flip from `passive` to `active` on the surviving pod, and
the client-facing Service (which selects on `role=active`) starts routing there without any
client-side retry logic.

## 4. Kill the Postgres primary — read availability vs. write recovery

This assignment's traffic is 100% reads, so the interesting result here is what *doesn't* break:

```bash
docker compose stop postgres-primary
curl -s "http://localhost:$LB_PORT/total?vehicleType=SUV&season=Summer&days=2"
# still 200 — Service B was already reading from a replica, which doesn't need the primary
# to keep serving already-replicated data
```

Promotion is what restores *write* capability (schema migrations, reseeding data) — not needed
to keep this specific read-only API up, but it's the recovery mechanism for the rest of the
system:

```bash
docker exec assignment_2-postgres-replica-1-1 psql -U vehicle_rental -d vehicle_rental -c "SELECT pg_promote();"
docker exec assignment_2-postgres-replica-1-1 psql -U vehicle_rental -d vehicle_rental -c "SELECT pg_is_in_recovery();"
# f — replica-1 is now a writable primary

docker compose start postgres-primary   # restart the old primary (it will not auto-rejoin
                                         # as a replica of the newly-promoted node — recovering
                                         # it properly is out of scope here; the takeaway is
                                         # that reads survived without any intervention)
```

## Running the same demo on kind (Kubernetes)

Everything above uses docker-compose. The same failure scenarios are more convincing on a real
Kubernetes cluster — in particular, scenario 3's `role` label flip becomes something you can
actually watch happen, and the client-facing `Service` re-routes on its own instead of you
checking two ports by hand.

### Setup

```bash
brew install kind   # installs the kind CLI; kubectl is commonly already installed (brew install kubectl if not)
kind create cluster --name vehicle-rental   # spins up a single-node Kubernetes cluster as a local Docker container

docker build -t vehicle-rental/service-vehicle-season-price:latest -f service-vehicle-season-price/Dockerfile .   # build Service B's image
docker build -t vehicle-rental/service-vehicle-total-price:latest -f service-vehicle-total-price/Dockerfile .     # build Service A's image
docker build -t vehicle-rental/load-balancer:latest -f load-balancer/Dockerfile .                                 # build the load balancer's image

kind load docker-image \
  vehicle-rental/service-vehicle-season-price:latest \
  vehicle-rental/service-vehicle-total-price:latest \
  vehicle-rental/load-balancer:latest \
  --name vehicle-rental
# copies the three locally-built images into kind's node — without this, kubelet would try
# (and fail) to pull them from a real registry, since they only exist on your machine

kubectl apply -f k8s/                 # creates every resource in the manifests: namespace, StatefulSets, Deployments, Services, RBAC
kubectl -n vehicle-rental get pods -w   # watch pod status live
# wait for everything Running/Ready — the two postgres-replica pods take the longest, since
# each is running pg_basebackup against the primary before Postgres itself even starts
```

In a separate terminal, port-forward to the client-facing Service (kind has no cloud load
balancer provisioner, so this replaces the `type: LoadBalancer` external IP):

```bash
kubectl -n vehicle-rental port-forward svc/load-balancer 8090:80
# forwards localhost:8090 to port 80 on the load-balancer Service — leave this running in its
# own terminal; it re-resolves to whichever pod is currently the active endpoint
```

Run the `curl` commands from scenarios 1 and 2 above unchanged, against `localhost:8090` — no
need to check two ports, since the Service only ever has the active pod as an endpoint.

### 3. Kill the active load balancer — the label flip, visible this time

```bash
kubectl -n vehicle-rental get pods -l app=load-balancer --show-labels   # confirm which pod is currently role=active

kubectl -n vehicle-rental get pods -l app=load-balancer,role=active -o name \
  | xargs kubectl -n vehicle-rental delete
# finds the pod labeled role=active and deletes it — simulates that instance crashing

kubectl -n vehicle-rental get pods -l app=load-balancer --show-labels -w
# watch role=active move to the surviving pod as it wins the re-election
```

The port-forward and `curl` loop from before keep working across this without you doing
anything — the Service's endpoint list updates the moment the label moves.

### 4. Kill the Postgres primary

```bash
kubectl -n vehicle-rental delete pod postgres-primary-0
# deletes the primary's pod — the StatefulSet recreates it, reattached to the same
# PersistentVolumeClaim; reads keep working throughout via postgres-replica, same as the
# compose scenario

kubectl -n vehicle-rental exec postgres-replica-0 -- \
  psql -U vehicle_rental -d vehicle_rental -c "SELECT pg_promote();"
# runs pg_promote() inside replica-0, ending its standby mode and making it a writable primary
```

### Teardown

```bash
kind delete cluster --name vehicle-rental   # tears down the whole local cluster and its containers
```
