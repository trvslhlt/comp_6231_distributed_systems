package cluster.management;

import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;


public class LeaderElection implements Watcher {
    private static final Logger logger = LoggerFactory.getLogger(LeaderElection.class);
    private static final String ELECTION_NAMESPACE = "/election";
    private ZooKeeper zooKeeper;
    private String currentZNodeName;
    private OnElectionCallback onElectionCallback;

    public LeaderElection(ZooKeeper zooKeeper, OnElectionCallback onElectionCallback) {
        this.zooKeeper = zooKeeper;
        this.onElectionCallback = onElectionCallback;
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
        String predecessorZNodeName = null;
        Stat predecessorStat = null;

        while (predecessorStat == null) {
            List<String> children = this.zooKeeper.getChildren(ELECTION_NAMESPACE, false);

            Collections.sort(children);
            String smallestChild = children.get(0);

            if (smallestChild.equals(this.currentZNodeName)) {
                logger.info("I am the leader");
                this.onElectionCallback.onElectedToBeLeader();
                return;
            }

            logger.info("I am not the leader, " + smallestChild + " is the leader");
            int predecessorIndex = Collections.binarySearch(children, currentZNodeName) - 1;
            predecessorZNodeName = children.get(predecessorIndex);
            predecessorStat = this.zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorZNodeName, this);
        }

        this.onElectionCallback.onWorker();
        logger.info("Watching znode: " + predecessorZNodeName);
    }

    // Watcher
    @Override
    public void process(WatchedEvent event) {
        logger.debug(event.toString());
        switch(event.getType()) {
            case NodeDeleted:
                try {
                    this.electLeader();
                } catch (InterruptedException e) {
                } catch (KeeperException e) {
                }
                break;
            default:
                logger.warn("unhandled event. type: " + event.getType());
        }
    }
}
