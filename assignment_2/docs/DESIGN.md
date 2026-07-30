# Design Decisions & Assumptions

This assignment's actual requirement is two microservices (Vehicle Season Price, Vehicle Total
Price) reading the provided pricing data. Everything below that isn't directly about those two
endpoints is the "additional features" the submission guidelines invite — added to explore the
course's distributed-systems themes, not because the base assignment demands it.

## Data model

- `vehicle_types(vehicle_type TEXT PRIMARY KEY)` — a real lookup table, not an enum, since this
  list is described as ~50 entries and open to growing.
- `season` as a native **Postgres ENUM** (`spring`, `summer`, `fall`, `winter`) — a genuinely
  closed, fixed domain that will never gain a 5th value, so a lookup table would just be a join
  for no benefit.
- `prices(vehicle_type, season, price_per_day)`, primary keyed on `(vehicle_type, season)`.
- Seed data is transcribed directly from the assignment's provided `02VEHI~1.JSON` JSON file
  (5 vehicle types × 4 seasons) via a Flyway migration.

Mapping a Java field to a Postgres native enum column through Hibernate has a well-known rough
edge (the driver doesn't have a first-class enum bind type). Rather than depend on Hibernate's
newer native-enum annotation support, `PriceEntity.season` is mapped as a plain `String` with
`columnDefinition = "season"`, and the JDBC URL carries `stringtype=unspecified` so pgjdbc sends
it untyped and Postgres coerces it — a standard, low-risk workaround for exactly this case.

## Why Postgres (not etcd) for the pricing data

etcd could technically hold this data too (it's tiny and read-mostly), but the brief specifically
asked for a real database with genuine replication/recovery semantics, so Postgres with streaming
replication is used instead, and etcd is reserved for coordination (see below).

## Postgres replication

Deliberately different implementations in the two environments, same as the load balancer's
active-passive routing (in-process guard everywhere, pod-label routing only in Kubernetes):

- **docker-compose**: 1 primary + 2 streaming-replication read replicas, using the vanilla
  `postgres:16-alpine` image (no Patroni) — a replica's custom entrypoint (`db/replica/`) runs
  `pg_basebackup ... -R` against the primary on first start, which writes `standby.signal` and
  `primary_conninfo` for us, then hands off to the normal Postgres entrypoint. Fault tolerance
  here is manual promotion only (`SELECT pg_promote()`), exercised in
  [../demo/compose/05-kill-postgres-primary.sh](../demo/compose/05-kill-postgres-primary.sh).
- **Kubernetes**: a single symmetric 3-node StatefulSet, all identical, managed by
  [Patroni](../db/patroni/) — Patroni itself decides who's primary via leader election over the
  same etcd cluster used for service discovery and load-balancer election, and automatically
  promotes a replica if the primary dies, with no manual `pg_promote()` needed. Failover is
  driven by the same SIGTERM-triggers-immediate-lock-release pattern used throughout this
  project (etcd's TTL is a backstop for an ungraceful crash, not the common case), so it's
  typically sub-second, not gated on the lease TTL. On every role change, Patroni runs a
  callback (`db/patroni/on_role_change.py`) that patches its own pod's `role` label — the exact
  same pattern `common/k8s/PodRoleLabeler` uses for the load balancer, just reimplemented in
  Python since this image is Patroni's, not Spring's. The `postgres-primary`/`postgres-replica`
  Services select on that label, so they always resolve to the current, correct pods. Verified
  live via repeated failovers: Patroni's own `/history` endpoint confirmed genuine timeline
  switches (not stale relabeling), and a fresh `CREATE TABLE`/`DROP TABLE` against
  `postgres-primary` succeeded immediately after each one with zero manual intervention — see
  [../demo/kind/04-kill-postgres-primary.sh](../demo/kind/04-kill-postgres-primary.sh).

In both environments, all runtime query traffic is reads, so Service B's `spring.datasource.*`
points at a replica (the `postgres-replica` Kubernetes Service load-balances across both;
docker-compose, lacking an equivalent, just pins each Service B instance to one specific
replica). Flyway migrations are configured as a **separate** connection (`spring.flyway.*`) that
always targets the primary, since replicas are read-only and can't run migrations — this stays
true regardless of which node is currently primary, in either environment, since Service A/B
address Postgres by Service name, never by pod identity.

## etcd's three jobs

A single 3-node etcd cluster (Raft quorum) backs:

1. **Service discovery** — Service A/B instances register `host:port` under a TTL lease in etcd
   and heartbeat via lease keep-alive (`common/discovery/EtcdServiceRegistry`); consumers watch
   the key prefix (`EtcdServiceDiscovery`). A dead instance's key disappears automatically when
   its lease expires — no separate health-check protocol.
2. **Load-balancer leader election** — etcd's fair/FIFO election recipe in
   `common/election/EtcdLeaderElection`: each candidate creates its own key (its own TTL lease)
   under a shared prefix, and whichever key has the lowest creation revision leads. A candidate
   that isn't first watches only the single key immediately ahead of it, so a leader's death
   wakes exactly one successor rather than every candidate racing at once — this avoids the
   thundering herd a single shared mutex key would cause as candidate count grows.
3. **Patroni's DCS backend for Postgres failover** (Kubernetes only — see above): a distinct
   key prefix (`PATRONI_SCOPE=vehicle-rental-pg`) keeps this fully isolated from the other two
   jobs' keys, despite sharing the same 3-node etcd cluster.

## Why Spring Cloud, and where

The professor mentioned Spring Cloud specifically. There's no official Spring Cloud etcd
integration, so etcd access itself is still hand-written (`jetcd` wrapped in `common`) — but
Spring Cloud's own abstractions sit on top of it rather than being bypassed:

- **Spring Cloud LoadBalancer**: Service A calls Service B via a logical name
  (`http://service-vehicle-season-price`) through a `@LoadBalanced RestClient`, resolved by a
  custom `ServiceInstanceListSupplier` that's fed from etcd instead of the usual DiscoveryClient.
  Verified live: repeated calls alternate `upstreamInstanceId` across two Service B instances,
  and stop routing to one within its lease TTL after it's killed.
- **Spring Cloud Gateway**: the load balancer is a Gateway app (route: `lb://service-vehicle-total-price`),
  not a hand-rolled proxy — Gateway is literally built for L7 reverse-proxying, so this is a
  direct fit rather than a stretch to use the library.

## Active-passive load balancer

Two load-balancer replicas run at all times; the etcd leader-election winner is "active." Two
things enforce this, deliberately redundant:

1. **In-process guard**: a Gateway `GlobalFilter` (`LeadershipGuardFilter`) returns `503` for
   every request unless this instance currently holds leadership.
2. **Kubernetes routing**: the winner patches its own pod's `role` label to `active` via the
   fabric8 Kubernetes client (`common/k8s/PodRoleLabeler`, using the pod's own ServiceAccount —
   see the RBAC `Role`/`RoleBinding` in `k8s/50-load-balancer.yaml`), and the client-facing
   `Service` selects strictly on `role=active`. The Deployment's own pod selector deliberately
   only matches `app=load-balancer` (not `role`), since a Deployment's selector must stay stable
   across its pods while `role` changes at runtime.

(1) alone would work in docker-compose, where there's no Kubernetes Service to filter on — that's
why both host ports are exposed there and the docs say "check which one answers 200." (2) is what
gives Kubernetes a single stable entry point instead of requiring clients to know to retry.
Verified live: killing the elected leader causes the other replica to win within the lease TTL
and immediately start serving `200`s.

## Other explicit assumptions

- **Java 21** for the new modules (Spring Boot 3.3.x), via `maven.compiler.release`, even though
  the rest of the repo (`leaderElection`, `serviceRegistry`, `webServer`) targets Java 25 — needed
  for solid Spring Boot 3.x support; the build/runtime JDK can still be newer.
- Ports: Service B `8081`, Service A `8080`, load balancer `8090` — arbitrary but fixed defaults,
  overridable via `SERVER_PORT`.
- Instance identity (`INSTANCE_ID`) defaults to a random UUID if not supplied; in Kubernetes it's
  set to the pod name via the Downward API for readable logs/responses.
