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
