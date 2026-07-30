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
| `port-forward.sh` | Run in its own terminal; forwards `localhost:8090` to the client-facing Service. Every other script here assumes this is running — **and must be re-run after scenario 3** (see the NOTE in the script; `kubectl port-forward` binds to one pod at start and doesn't follow the Service's endpoint changes, unlike the Service itself). |
| `01-golden-path.sh` | Same as compose, against the single Service entrypoint — no port-checking needed |
| `02-kill-service-b.sh` | The deregistration side of service discovery, via `kubectl scale` instead of `docker compose kill` — see the NOTE in the script for why a true crash is surprisingly hard to reproduce faithfully in Kubernetes (the lease-expiry path specifically is better observed via `../compose/03-kill-service-b.sh`) |
| `03-kill-load-balancer.sh` | The `role` label flip, watched live |
| `04-kill-postgres-primary.sh` | Automatic Postgres failover via Patroni — no `pg_promote()` anywhere in this script, unlike the compose version. A different node is elected primary (confirmed via Patroni's own `/history` endpoint, not just a relabel), and a fresh write against `postgres-primary` succeeds immediately after, all with zero manual intervention. |
| `05-teardown.sh` | Tears down the whole kind cluster |

```bash
cd demo/kind
./00-setup.sh
./port-forward.sh &   # or run in a separate terminal
./01-golden-path.sh
./02-kill-service-b.sh
./03-kill-load-balancer.sh
./04-kill-postgres-primary.sh
./05-teardown.sh
```
