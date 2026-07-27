# Vehicle Rental Price Calculation System

Implements the assignment's two required microservices — **Vehicle Season Price** and
**Vehicle Total Price** — using the JSON pricing dataset provided in `assignment/`. Beyond the
base requirement, this project explores the distributed-systems themes of the course as
"additional features": each service runs as N instances, Postgres is set up with streaming
replication for fault tolerance, an active-passive Layer 7 load balancer fronts the client-facing
service, etcd provides service discovery and leader election, and the whole system is deployable
to Kubernetes.

See `assignment/` for the original assignment brief and provided dataset.

## Architecture

Solid arrows are the client request path; dotted arrows are etcd coordination (registration,
discovery, leader election) — a separate concern from the request path itself.

```mermaid
flowchart LR
    client([HTTP client])

    subgraph LB["Load Balancer (active-passive)"]
        lbA[["lb-1 — active"]]
        lbP[["lb-2 — passive, returns 503"]]
    end

    subgraph SA["Service A — Vehicle Total Price (N)"]
        a1[total-price-1]
        a2[total-price-2]
    end

    subgraph SB["Service B — Vehicle Season Price (N)"]
        b1[season-price-1]
        b2[season-price-2]
    end

    subgraph PG["Postgres"]
        pgPrimary[(primary)]
        pgR1[(replica-1)]
        pgR2[(replica-2)]
    end

    subgraph ETCD["etcd cluster (3 nodes, Raft)"]
        e0[(etcd-0)]
        e1[(etcd-1)]
        e2[(etcd-2)]
    end

    client -->|GET /total| lbA
    lbA --> a1
    lbA --> a2
    a1 --> b1
    a1 --> b2
    a2 --> b1
    a2 --> b2
    b1 --> pgR1
    b2 --> pgR2
    pgPrimary ==>|streaming replication| pgR1
    pgPrimary ==>|streaming replication| pgR2

    lbA -.->|leader election| e0
    a1 -.->|discover Service B| e0
    b1 -.->|register| e0

    classDef active fill:#2ecc71,stroke:#1a7a41,color:#04210f;
    classDef passive fill:#95a5a6,stroke:#5f6a6a,color:#1c1f1f;
    class lbA active
    class lbP passive
```

etcd (a 3-node Raft cluster in Kubernetes; a single node in the docker-compose dev stack) is the
coordination backbone underneath all three of: service discovery, load-balancer leader election,
and (optionally) Postgres failover via Patroni — see [docs/DESIGN.md](docs/DESIGN.md).

## Modules

| Module | Role |
|---|---|
| `common` | Shared DTOs, etcd-backed service registry/discovery ([jetcd](https://github.com/etcd-io/jetcd)), leader election, and Kubernetes pod-labeling helper ([fabric8](https://github.com/fabric8io/kubernetes-client)) |
| `service-vehicle-season-price` | Service B: looks up the daily rate for a vehicle type + season from Postgres |
| `service-vehicle-total-price` | Service A: calls Service B and multiplies by a rental period |
| `load-balancer` | Active-passive Spring Cloud Gateway in front of Service A |

## Documentation

- [docs/API.md](docs/API.md) — endpoints, example requests/responses
- [docs/SETUP.md](docs/SETUP.md) — running locally, via docker-compose, and on Kubernetes
- [docs/DESIGN.md](docs/DESIGN.md) — design decisions and assumptions
- [docs/DEMO.md](docs/DEMO.md) — fault-injection walkthrough for the presentation

## Quickest path to a running system

```bash
docker compose up --build -d
curl "http://localhost:8090/total?vehicleType=SUV&season=Winter&days=10"
# if that returns 503, the other load-balancer replica is the active one:
curl "http://localhost:8091/total?vehicleType=SUV&season=Winter&days=10"
```

Full instructions, including single-instance local dev and Kubernetes, are in
[docs/SETUP.md](docs/SETUP.md).
