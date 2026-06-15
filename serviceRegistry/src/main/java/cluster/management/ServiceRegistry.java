package cluster.management;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceRegistry implements Watcher {
    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);
    private static final String REGISTRY_ZNODE = "/service_registry";
    private final ZooKeeper zooKeeper;
    private String currentZNode = null;
    private List<String> allServiceAddresses = null;

    public ServiceRegistry(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
        this.createServiceRegistryZNode();
    }

    private void createServiceRegistryZNode() {
        try {
            if (this.zooKeeper.exists(REGISTRY_ZNODE, false) == null) {
                this.zooKeeper.create(REGISTRY_ZNODE, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void registerToCluster(String metadata) throws KeeperException, InterruptedException {
        if (this.currentZNode != null) {
            logger.info("ZNode already registered to service registry: " + this.currentZNode);
            return;
        }
        this.currentZNode = this.zooKeeper.create(
            REGISTRY_ZNODE + "/n_", 
            metadata.getBytes(), 
            ZooDefs.Ids.OPEN_ACL_UNSAFE, 
            CreateMode.EPHEMERAL_SEQUENTIAL);
        logger.info("ZNode registered to service registry: " + this.currentZNode);
    }

    public void unreigsterFromCluster() {
        try {
            if (this.currentZNode != null && this.zooKeeper.exists(this.currentZNode, false) != null) {
                this.zooKeeper.delete(this.currentZNode, -1);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private synchronized void updateAddresses() throws KeeperException, InterruptedException {
        List<String> workerZNodes = this.zooKeeper.getChildren(REGISTRY_ZNODE, this);
        List<String> addresses = new ArrayList<>(workerZNodes.size());

        for (String workerZNode : workerZNodes) {
            String workerFullPath = REGISTRY_ZNODE + "/" + workerZNode;
            Stat stat = this.zooKeeper.exists(workerFullPath, false);
            if (stat == null) {
                continue;
            }

            byte[] addressBytes = this.zooKeeper.getData(workerFullPath, false, stat);
            String address = new String(addressBytes);
            addresses.add(address);
        }

        this.allServiceAddresses = Collections.unmodifiableList(addresses);
        logger.info("Cluster addresses are: " + this.allServiceAddresses);
    }

    private synchronized List<String> getAllServiceAddresses() throws KeeperException, InterruptedException {
        if (this.allServiceAddresses == null) {
            this.updateAddresses();
        }
        return this.allServiceAddresses;
    }

    public void registerForUpdates() {
        try {
            this.updateAddresses();
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Watcher
    @Override
    public void process(WatchedEvent event) {
        try {
            this.updateAddresses();
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
