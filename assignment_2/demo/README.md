# Presentation Demo Scripts

Runnable fault-injection scenarios for both deployment targets. All of this was exercised
directly while building the system, not just written down.

## docker-compose

Bring the stack up first ([../docs/SETUP.md](../docs/SETUP.md), Option 2), then from
`demo/compose/`:

| Script | Demonstrates |
|---|---|
| `01-find-active-lb.sh` | Prints whichever load-balancer port (8090/8091) is currently active. Used by the other scripts, but also runnable standalone. |
| `02-golden-path.sh` | Load balancing across N instances of both services |
| `03-kill-service-b.sh` | Service discovery + fault detection when a Service B instance dies |
| `04-kill-load-balancer.sh` | Leader election failover when the active load balancer dies |
| `05-kill-postgres-primary.sh` | Read availability survives a primary failure; write-capability recovery via promotion |

```bash
cd demo/compose
./02-golden-path.sh
./03-kill-service-b.sh
./04-kill-load-balancer.sh
./05-kill-postgres-primary.sh
```

## Kubernetes (kind)

The same scenarios are more convincing on a real cluster — in particular, scenario 3's `role`
label flip becomes something you can watch happen, and the client-facing `Service`'s endpoint
list re-routes on its own instead of you checking two ports by hand. From `demo/kind/`:

| Script | Demonstrates |
|---|---|
| `00-setup.sh` | Creates a kind cluster, builds + loads all four images, applies `k8s/`, waits for everything Ready |
| `01-port-forward.sh` | Run in its own terminal; forwards `localhost:8090` to the client-facing Service. Every other script here assumes this is running — **and must be re-run after scenario 4** (see the NOTE in the script; `kubectl port-forward` binds to one pod at start and doesn't follow the Service's endpoint changes, unlike the Service itself). |
| `02-golden-path.sh` | Same as compose, against the single Service entrypoint — no port-checking needed |
| `03-kill-service-b.sh` | The deregistration side of service discovery, via `kubectl scale` instead of `docker compose kill` — see the NOTE in the script for why a true crash is surprisingly hard to reproduce faithfully in Kubernetes (the lease-expiry path specifically is better observed via `../compose/03-kill-service-b.sh`) |
| `04-kill-load-balancer.sh` | The `role` label flip, watched live |
| `05-kill-postgres-primary.sh` | Automatic Postgres failover via Patroni — no `pg_promote()` anywhere in this script, unlike the compose version. A different node is elected primary (confirmed via Patroni's own `/history` endpoint, not just a relabel), and a fresh write against `postgres-primary` succeeds immediately after, all with zero manual intervention. |
| `06-teardown.sh` | Tears down the whole kind cluster |

### Inspecting cluster state

Each script already prints the interesting output inline, but these are useful to run alongside
(in another terminal) for a live audience, or afterward to poke around by hand:

| After running... | ...inspect with |
|---|---|
| `00-setup.sh` | `kubectl -n vehicle-rental get pods,svc,statefulsets,deployments` — confirms every workload and Service exists and is Ready |
| `02-golden-path.sh` | `kubectl -n vehicle-rental get pods -l app=service-vehicle-total-price -o wide` — cross-check the pod names against the `servedBy`/`upstream` values the script just printed |
| `03-kill-service-b.sh` | `kubectl -n vehicle-rental get pods -l app=service-vehicle-season-price -w` — watch the replica count drop to 2, then a new one appear when the script scales back to 3 |
| `04-kill-load-balancer.sh` | `kubectl -n vehicle-rental get pods -l app=load-balancer --show-labels -w` — watch `role=active` move to the surviving pod in real time |
| `05-kill-postgres-primary.sh` | `kubectl -n vehicle-rental get pods -l app=postgres --show-labels` and `kubectl -n vehicle-rental get endpoints postgres-primary postgres-replica` — confirm the pod label and the Service's endpoint moved to the same new primary |
| `06-teardown.sh` | `kind get clusters` — should print "No kind clusters found" |

```bash
cd demo/kind
./00-setup.sh
./01-port-forward.sh &   # or run in a separate terminal
./02-golden-path.sh
./03-kill-service-b.sh
./04-kill-load-balancer.sh
./05-kill-postgres-primary.sh
./06-teardown.sh
```
