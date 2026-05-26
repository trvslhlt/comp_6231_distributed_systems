package com.example.zk;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Demonstrates the core ZooKeeper operations via the native Java client: connect → create → read →
 * update → watch → list → delete → disconnect
 *
 * Assumes the ensemble from zookeeper_example/docker-compose.yml is running: docker compose -f
 * ../zookeeper_example/docker-compose.yml up -d
 */
public class ZooKeeperDemo {

    private static final Logger log = LoggerFactory.getLogger(ZooKeeperDemo.class);

    private static final String CONNECT_STRING = "localhost:2181,localhost:2182,localhost:2183";
    private static final int SESSION_TIMEOUT_MS = 3000;
    private static final String BASE_PATH = "/demo";

    public static void main(String[] args) throws Exception {
        ZooKeeperDemo demo = new ZooKeeperDemo();
        demo.run();
    }

    private void run() throws Exception {
        ZooKeeper zk = connect();
        try {
            cleanup(zk, BASE_PATH);

            // ── Create ────────────────────────────────────────────────────────
            create(zk, BASE_PATH, "hello from java");
            create(zk, BASE_PATH + "/child1", "child one");
            create(zk, BASE_PATH + "/child2", "child two");

            // ── Read ──────────────────────────────────────────────────────────
            read(zk, BASE_PATH);

            // ── Update ────────────────────────────────────────────────────────
            update(zk, BASE_PATH, "updated value");
            read(zk, BASE_PATH);

            // ── Watch ─────────────────────────────────────────────────────────
            watch(zk, BASE_PATH);

            // ── List children ─────────────────────────────────────────────────
            listChildren(zk, BASE_PATH);

            // ── Check existence ───────────────────────────────────────────────
            exists(zk, BASE_PATH);
            exists(zk, BASE_PATH + "/nonexistent");

            // ── Delete ────────────────────────────────────────────────────────
            deleteTree(zk, BASE_PATH);
            exists(zk, BASE_PATH);

        } finally {
            zk.close();
            log.info("disconnected");
        }
    }

    // ── Connection ────────────────────────────────────────────────────────────

    private ZooKeeper connect() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);

        ZooKeeper zk = new ZooKeeper(CONNECT_STRING, SESSION_TIMEOUT_MS, event -> {
            if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                connected.countDown();
            }
        });

        connected.await();
        log.info("connected  session=0x{}  state={}", Long.toHexString(zk.getSessionId()),
                zk.getState());
        return zk;
    }

    // ── CRUD operations ───────────────────────────────────────────────────────

    private void create(ZooKeeper zk, String path, String data) throws Exception {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);

        // CreateMode options:
        // PERSISTENT – survives client disconnect
        // PERSISTENT_SEQUENTIAL – persistent + auto-incremented suffix
        // EPHEMERAL – deleted when the creating session ends
        // EPHEMERAL_SEQUENTIAL – ephemeral + auto-incremented suffix
        String created = zk.create(path, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        log.info("create  path={}  data=\"{}\"", created, data);
    }

    private void read(ZooKeeper zk, String path) throws Exception {
        Stat stat = new Stat();
        byte[] bytes = zk.getData(path, false, stat);
        String value = new String(bytes, StandardCharsets.UTF_8);
        log.info("read    path={}  data=\"{}\"  version={}", path, value, stat.getVersion());
    }

    private void update(ZooKeeper zk, String path, String newData) throws Exception {
        Stat stat = zk.exists(path, false);
        if (stat == null) {
            log.warn("update  path={} not found", path);
            return;
        }
        // version must match the current version; pass -1 to skip the check
        zk.setData(path, newData.getBytes(StandardCharsets.UTF_8), stat.getVersion());
        log.info("update  path={}  data=\"{}\"", path, newData);
    }

    // ── Watch ─────────────────────────────────────────────────────────────────

    private void watch(ZooKeeper zk, String path) throws Exception {
        CountDownLatch eventReceived = new CountDownLatch(1);

        // Registering a one-shot watcher via getData; fires on the next change
        zk.getData(path, event -> {
            log.info("watch   event: type={}  path={}", event.getType(), event.getPath());
            eventReceived.countDown();
        }, null);

        // Trigger the watch by modifying the node
        zk.setData(path, "watch trigger".getBytes(StandardCharsets.UTF_8), -1);

        eventReceived.await();
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private void listChildren(ZooKeeper zk, String path) throws Exception {
        List<String> children = zk.getChildren(path, false);
        log.info("children  path={}  children={}", path, children);
    }

    private void exists(ZooKeeper zk, String path) throws Exception {
        Stat stat = zk.exists(path, false);
        if (stat != null) {
            log.info("exists  path={}  YES  (version={}, numChildren={})", path, stat.getVersion(),
                    stat.getNumChildren());
        } else {
            log.info("exists  path={}  NO", path);
        }
    }

    /** Recursively delete a subtree. */
    private void deleteTree(ZooKeeper zk, String path) throws Exception {
        List<String> children = zk.getChildren(path, false);
        for (String child : children) {
            deleteTree(zk, path + "/" + child);
        }
        zk.delete(path, -1);
        log.info("delete  path={}", path);
    }

    /** Remove the demo subtree if it was left over from a previous run. */
    private void cleanup(ZooKeeper zk, String path) throws Exception {
        if (zk.exists(path, false) != null) {
            log.info("cleanup  removing leftover {}", path);
            deleteTree(zk, path);
        }
    }
}
