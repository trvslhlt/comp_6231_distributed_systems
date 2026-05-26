# ZooKeeper Ensemble Example

A 3-node ZooKeeper ensemble running in Docker. Use this as a sandbox for learning ZooKeeper concepts: quorum, leader election, znodes, watches, ephemeral nodes, and fault tolerance.

---

## What is ZooKeeper?

ZooKeeper is a coordination service for distributed systems. It gives multiple processes running on different machines a single, consistent place to share small pieces of state — who is the leader, which nodes are alive, what the current config is, who holds a lock.

It is not a database for application data. It is purpose-built for coordination primitives: the data is tiny, but the guarantees are strong.

---

## Architecture

```
Your machine (host)
   │
   │  localhost:2181 ──────────────────────────────┐
   │  localhost:2182 ───────────────────────────┐  │
   │  localhost:2183 ────────────────────────┐  │  │
   │                                         │  │  │
   │  ./logs/zoo1   ──────────────────────┐  │  │  │
   │  ./logs/zoo2   ───────────────────┐  │  │  │  │
   │  ./logs/zoo3   ────────────────┐  │  │  │  │  │
   │                                │  │  │  │  │  │
   └────────────────────────────────┼──┼──┼──┼──┼──┘
                         Docker     │  │  │  │  │
        ┌──────────────────────────────────────────────────┐
        │  zknet (bridge network)   │  │  │  │  │          │
        │                           ▼  │  │  ▼  │          │
        │  ┌─────────────────────┐     │  │     │          │
        │  │        zoo1         │◄────┘  │     │          │
        │  │  data: /logs        │        │     │          │
        │  │  client:  2181      │◄───────┘     │          │
        │  │  peer:    2888      │              │          │
        │  │  election:3888      │◄─────────────┘          │
        │  └─────────────────────┘                         │
        │                                                  │
        │  ┌─────────────────────┐  ┌─────────────────────┐│
        │  │        zoo2         │  │        zoo3         ││
        │  │  data: /logs        │  │  data: /logs        ││
        │  │  client:  2181      │  │  client:  2181      ││
        │  │  peer:    2888      │  │  peer:    2888      ││
        │  │  election:3888      │  │  election:3888      ││
        │  └─────────────────────┘  └─────────────────────┘│
        │                                                  │
        │  ┌──────────────────────────────────────────┐    │
        │  │  zkclient  (run on demand, auto-removed) │    │
        │  └──────────────────────────────────────────┘    │
        └──────────────────────────────────────────────────┘
```

### Ports

| Port (internal) | Purpose |
|---|---|
| `2181` | Client connections — this is the port your application uses |
| `2888` | Follower-to-leader communication (data replication) |
| `3888` | Leader election — only used when voting for a new leader |

Each node's `2181` is also mapped to the host (`2181`, `2182`, `2183`) so you can connect local tools directly.

### Volumes

Each node mounts its own local `logs/` subdirectory to `/logs` inside the container. This is ZooKeeper's `dataDir` — where it stores:

- `myid` — a single file containing this node's ID (1, 2, or 3)
- `version-2/` — transaction logs and snapshots (ZooKeeper's persistent state)

Data survives container restarts because it lives on your host machine.

### Quorum

With 3 nodes, quorum is 2. The cluster tolerates 1 node failure. A write is only committed once a majority of nodes acknowledge it.

| Nodes | Quorum | Failures tolerated |
|---|---|---|
| 1 | 1 | 0 |
| 3 | 2 | **1** |
| 5 | 3 | **2** |

Even numbers are wasteful — 4 nodes still only tolerates 1 failure. ZooKeeper ensembles are always odd-numbered.

---

## Managing the Ensemble

### Start

```bash
docker compose up -d
```

### Stop (keeps data)

```bash
docker compose down
```

### Stop and wipe all data

```bash
docker compose down
rm -rf logs/zoo1/* logs/zoo2/* logs/zoo3/*
```

### Check which node is the leader

```bash
docker compose exec zoo1 bin/zkServer.sh status
docker compose exec zoo2 bin/zkServer.sh status
docker compose exec zoo3 bin/zkServer.sh status
```

One reports `Mode: leader`, the others `Mode: follower`.

### View logs for a node

```bash
docker compose logs zoo1
docker compose logs -f zoo1   # follow live
```

---

## Interactive Client Shell

```bash
docker compose run --rm zkclient
```

This drops you into `zkCli.sh` connected to all three nodes. The `--rm` flag removes the container when you exit. If one node is down, the client automatically uses the others.

You will see a connection message like:

```
Session establishment complete on server zoo3/172.19.0.4:2181
session id = 0x3000000e6410000, negotiated timeout = 30000
WatchedEvent state:SyncConnected type:None path:null
```

- **SyncConnected** — client is fully synced with the leader and ready
- **session id** — your client's unique session; ephemeral nodes you create are tied to this
- **negotiated timeout** — if your client goes silent for 30 seconds, ZooKeeper treats it as dead and deletes its ephemeral nodes

---

## ZooKeeper Concepts and Commands

### Znode types

All data in ZooKeeper lives in znodes — nodes in a tree structure, like a filesystem. There are four types:

| Type | Flag | Survives disconnect | Auto-numbered |
|---|---|---|---|
| Persistent | *(none)* | yes | no |
| Ephemeral | `-e` | no | no |
| Persistent Sequential | `-s` | yes | yes |
| Ephemeral Sequential | `-e -s` | no | yes |

### Basic operations

```bash
# Create a persistent znode with data
create /myapp "hello"

# Read data
get /myapp

# Update data
set /myapp "world"

# List children of a path
ls /myapp

# Delete (node must have no children)
delete /myapp

# Delete recursively
deleteall /myapp

# See metadata (version, timestamps, child count)
stat /myapp
```

### Watches

A watch is a one-time notification. When the watched path changes, ZooKeeper pushes an event to the client. This is how distributed components react to state changes without polling.

Open two client shells. In shell 1:

```bash
get -w /myapp
```

In shell 2:

```bash
set /myapp "changed"
```

Shell 1 receives a `NodeDataChanged` event immediately. The watch is then consumed — set it again if you want to keep watching.

You can also watch for children appearing or disappearing:

```bash
ls -w /myapp
```

### Ephemeral nodes

```bash
create -e /myapp/session "temporary"
```

Deleted automatically when the client session ends (disconnect or timeout). Used for:

- **Membership tracking** — each node in your system creates an ephemeral znode on startup. When it crashes, the node disappears. Other nodes watch the parent path and see the change.
- **Leader detection** — followers watch the leader's ephemeral node. When it disappears, they know to start an election.

### Sequential nodes

```bash
create -s /myapp/lock- ""
create -s /myapp/lock- ""
create -s /myapp/lock- ""
```

ZooKeeper appends a zero-padded counter, producing `/myapp/lock-0000000001`, `/myapp/lock-0000000002`, etc. The counter is global to the parent node and never resets.

Combining ephemeral + sequential is the standard recipe for **distributed locks and leader election**:

1. Every candidate creates an ephemeral sequential znode under a common path
2. The node with the lowest sequence number holds the lock / is the leader
3. Each other node watches only the node immediately ahead of it in the sequence
4. When the leader/lock-holder dies, its ephemeral node disappears, and only the next-in-line gets notified — no thundering herd

---

## Simulating Failures

### Kill one node — quorum survives

```bash
docker compose stop zoo1
```

The cluster still has 2 of 3 nodes and keeps serving requests. Any client connected to zoo1 will reconnect to another node automatically.

### Rejoin the node

```bash
docker compose start zoo1
```

zoo1 reconnects, syncs its transaction log from the leader, and rejoins without any manual intervention.

### Force a leader re-election

```bash
# 1. Find the current leader
docker compose exec zoo1 bin/zkServer.sh status
docker compose exec zoo2 bin/zkServer.sh status
docker compose exec zoo3 bin/zkServer.sh status

# 2. Stop the leader (replace N with its number)
docker compose stop zooN

# 3. Watch the remaining nodes elect a new leader
docker compose exec zoo<other> bin/zkServer.sh status
```

Election completes in seconds. Restart the stopped node and it rejoins as a follower.

### Split brain scenario (lose quorum)

```bash
docker compose stop zoo1
docker compose stop zoo2
```

With only 1 of 3 nodes, quorum is lost. The remaining node stops accepting writes and the cluster is unavailable. This is intentional — ZooKeeper prefers consistency over availability.

```bash
# Restore quorum
docker compose start zoo1
```
