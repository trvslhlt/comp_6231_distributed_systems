# Service Registry

A service registry and discovery implementation using Apache ZooKeeper.

## Prerequisites

- Java 17+
- Maven
- ZooKeeper running on `localhost:2181`

## Architecture

Multiple instances connect to ZooKeeper and compete for leadership. Each startup
volunteers for leadership by creating an ephemeral sequential znode under `/election`.
The instance holding the lowest sequence number is the leader.

```
                      ┌──────────────────┐
       ┌──────────────│    ZooKeeper     │──────────────┐
       │              │  localhost:2181  │              │
       │              └────────┬─────────┘              │
       │                       │                        │
       ▼                       ▼                        ▼
 ┌───────────┐          ┌───────────┐           ┌───────────┐
 │Instance A │          │Instance B │           │Instance C │
 │  :8080    │          │  :8081    │           │  :8082    │
 │  LEADER   │          │  worker   │           │  worker   │
 └───────────┘          └───────────┘           └───────────┘
```

ZooKeeper holds two trees: `/election` tracks leadership order; `/service_registry`
holds the HTTP addresses of all active workers.

```
/
├── election/                              (ephemeral sequential znodes)
│   ├── c_0000001  ← Instance A           leader — has lowest sequence number
│   ├── c_0000002  ← Instance B           watches c_0000001
│   └── c_0000003  ← Instance C           watches c_0000002
│
└── service_registry/                     (workers register their HTTP address)
    ├── n_0000001  →  "http://hostB:8081"
    └── n_0000002  →  "http://hostC:8082"
```

Each worker watches only its direct predecessor in `/election` (not the leader),
avoiding a herd effect on leader failure. When the leader's znode is deleted,
only the next-in-line is notified and re-runs the election. The new leader then
unregisters from `/service_registry` and begins watching it for worker changes.

## Build and Run

```bash
mvn clean compile exec:java
```

To specify a port (default: `8080`):

```bash
mvn clean compile exec:java -Dexec.args="<port>"
```

## Package

```bash
mvn clean package
```

## Run Packaged JAR

```bash
java -jar target/leader.election-1.0-SNAPSHOT-jar-with-dependencies.jar
```

To specify a port (default: `8080`):

```bash
java -jar target/leader.election-1.0-SNAPSHOT-jar-with-dependencies.jar <port>
```


