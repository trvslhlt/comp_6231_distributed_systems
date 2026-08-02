# Vehicle Rental Price Calculation System

Implements the two required microservices: **Vehicle Season Price** and **Vehicle Total Price**. 

Beyond the base requirement, this project explores distributed-systems course themes as "additional features". All architectural decisions are my own, while **not required** features were coded with the help of Claude:
- Each service runs as N instances
- Postgres is set up with streaming replication for fault tolerance
- The application is fronted by an active-passive Layer 7 load balancer
- etcd provides service discovery and leader election
- The whole system is deployable to Kubernetes

## Architecture

### Request flow

Solid arrows are the client request path; dotted arrows are etcd coordination.

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

etcd (3-node cluster in k8s; a single node in the docker-compose stack) is the coordination technology backing service discovery and leader election everywhere, including Postgres failover via Patroni in the Kubernetes deployment (docker-compose's Postgres setup only supports manual promotion).

### Kubernetes resource topology

This shows how each piece is packaged and wired up inside the k8s cluster.

```mermaid
flowchart TB
    external([External client])

    subgraph NS["Namespace: vehicle-rental"]
        subgraph LBK["Load Balancer"]
            lbSvc["Service<br/>type: LoadBalancer<br/>selector: role=active"]
            lbPods["Deployment: load-balancer<br/>replicas: 2"]
            lbRBAC["ServiceAccount + Role<br/>(patch own pod's role label)"]
        end

        subgraph SAK["Service A"]
            aSvc["Service"]
            aPods["Deployment<br/>replicas: N"]
        end

        subgraph SBK["Service B"]
            bSvc["Service"]
            bPods["Deployment<br/>replicas: N"]
        end

        subgraph PGK["Postgres (Patroni)"]
            pgPrimarySvc["Service: postgres-primary<br/>selector: role=primary"]
            pgReplicaSvc["Service: postgres-replica<br/>selector: role=replica"]
            pgPods["StatefulSet: postgres<br/>replicas: 3 (symmetric —<br/>Patroni elects the primary)"]
            pgRBAC["ServiceAccount + Role<br/>(patch own pod's role label)"]
        end

        subgraph ETCDK["etcd"]
            etcdSvc["Service (headless)"]
            etcdPods["StatefulSet<br/>replicas: 3"]
        end
    end

    external --> lbSvc
    lbSvc --> lbPods
    lbPods --- lbRBAC
    lbPods -.->|pod IP via etcd, not aSvc| aPods
    aSvc --- aPods
    aPods -.->|pod IP via etcd, not bSvc| bPods
    bSvc --- bPods
    bPods --> pgReplicaSvc
    pgReplicaSvc --> pgPods
    bPods -.->|Flyway migrations only| pgPrimarySvc
    pgPrimarySvc --> pgPods
    pgPods --- pgRBAC
    pgPods -.->|leader election| etcdSvc
    lbPods -.->|leader election| etcdSvc
    aPods -.->|discovery| etcdSvc
    bPods -.->|registration| etcdSvc
    etcdSvc --> etcdPods

    classDef hotpath fill:#3498db,stroke:#1f5f8b,color:#eef6fc;
    class lbSvc,pgPrimarySvc,pgReplicaSvc,etcdSvc hotpath
```

## Modules

| Module | Role |
|---|---|
| `common` | Shared components |
| `service-vehicle-season-price` | API service: reads pricing data from Postgres |
| `service-vehicle-total-price` | API service: fetches data from season price API and computes total |
| `load-balancer` | Active-passive Spring Cloud Gateway in front of total price API |

## Documentation

- [docs/API.md](docs/API.md) — endpoints, example requests/responses
- [docs/SETUP.md](docs/SETUP.md) — how to run the project locally
- [demo/](demo/) — runnable fault-injection scripts for the presentation
