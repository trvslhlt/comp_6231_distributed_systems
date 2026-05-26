# ZooKeeper Java Client Demo

A minimal Maven project demonstrating how to interact with ZooKeeper from Java using the native client library.

## What it illustrates

| Operation | API call |
|---|---|
| Connect to ensemble | `new ZooKeeper(connectString, timeout, watcher)` |
| Create a znode | `zk.create(path, data, acl, CreateMode)` |
| Read a znode | `zk.getData(path, watcher, stat)` |
| Update a znode | `zk.setData(path, data, version)` |
| Watch for changes | one-shot `Watcher` on `getData`, fired on the next write |
| List children | `zk.getChildren(path, watcher)` |
| Check existence | `zk.exists(path, watcher)` |
| Delete (recursive) | `zk.delete(path, version)` |

The demo also shows the four `CreateMode` variants (persistent, ephemeral, and both sequential forms) and optimistic concurrency via the `version` parameter on writes.

## Prerequisites

- Java 17+
- Maven 3.x
- The ZooKeeper ensemble from `../zookeeper_example` running

## Usage

**1. Start the ensemble**
```bash
docker compose -f ../zookeeper_example/docker-compose.yml up -d
```

**2. Compile and run**
```bash
mvn compile exec:java
```

**3. Stop the ensemble when done**
```bash
docker compose -f ../zookeeper_example/docker-compose.yml down
```

## Connecting to a different ensemble

Edit the `CONNECT_STRING` constant in [ZooKeeperDemo.java](src/main/java/com/example/zk/ZooKeeperDemo.java):

```java
private static final String CONNECT_STRING = "localhost:2181,localhost:2182,localhost:2183";
```
