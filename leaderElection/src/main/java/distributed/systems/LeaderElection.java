package distributed.systems;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;


public class LeaderElection implements Watcher {
    private static final Logger logger = Logger.getLogger(LeaderElection.class);
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000; // time before considering client dead
    private static final String ELECTION_NAMESPACE = "/election";
    private static final String TARGET_ZNODE = "/target_znode";
    private ZooKeeper zooKeeper;
    private String currentZNodeName;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        logger.info("Running 'main'");
        
        LeaderElection leaderElection = new LeaderElection();
        leaderElection.connectToZooKeeper();
        leaderElection.volunteerForLeadership();
        leaderElection.electLeader();
        leaderElection.run();
        leaderElection.close();

        logger.info("Disconnected from ZooKeeper. Exiting Applicaiton.");
    }

    public void run() throws InterruptedException {
        synchronized (this.zooKeeper) {
            this.zooKeeper.wait();
        }
    }

    public void connectToZooKeeper() throws IOException{
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }

    public void close() throws InterruptedException {
        this.zooKeeper.close();
    }

    public void volunteerForLeadership() throws KeeperException, InterruptedException {
        String zNodePrefix = ELECTION_NAMESPACE + "/c_";
        String zNodeFullPath = this.zooKeeper.create(
            zNodePrefix, 
            new byte[]{}, 
            ZooDefs.Ids.OPEN_ACL_UNSAFE, 
            CreateMode.EPHEMERAL_SEQUENTIAL);
        logger.info("znode name: " + zNodeFullPath);
        this.currentZNodeName = zNodeFullPath.replace("/election/", "");
    }

    public void electLeader() throws KeeperException, InterruptedException {
        List<String> children = this.zooKeeper.getChildren(ELECTION_NAMESPACE, false);
        Collections.sort(children);
        String smallestChild = children.get(0);
        if (smallestChild.equals(this.currentZNodeName)) {
            logger.info("I am the leader");
            return;
        }
        logger.info("I am not the leader, " + smallestChild + " is the leader");
    }

    public void watchTargetZNode() throws KeeperException, InterruptedException {
        logger.debug("watch target znode");
        Stat stat = this.zooKeeper.exists(TARGET_ZNODE, this);
        logger.info("Watching " + TARGET_ZNODE + ", stat: " + stat);
        if (stat == null) {
            return;
        }

        byte[] data = this.zooKeeper.getData(TARGET_ZNODE, this, stat);
        List<String> children = this.zooKeeper.getChildren(TARGET_ZNODE, this);

        logger.info("Data: " + new String(data) + ", children: " + children);
    }

    // Watcher
    @Override
    public void process(WatchedEvent event) {
        logger.debug(event);
        switch(event.getType()) {
            case None:
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    logger.info("Successfully connected to ZooKeeper");
                } else {
                    synchronized (this.zooKeeper) {
                        logger.info("Disconnected from ZooKeeper event");
                        this.zooKeeper.notifyAll();
                    }
                }
                break;
            case NodeDeleted:
                logger.info(TARGET_ZNODE + " was deleted");
                break;
            case NodeCreated:
                logger.info(TARGET_ZNODE + " was created");
                break;
            case NodeDataChanged:
                logger.info(TARGET_ZNODE + " data changed");
                break;
            case NodeChildrenChanged:
                logger.info(TARGET_ZNODE + " children changed");
                break;
            default:
                logger.warn("unhandled event. type: " + event.getType());
        }

        try {
            this.watchTargetZNode();
        } catch (KeeperException e) {

        } catch (InterruptedException e) {

        }
    }
}
