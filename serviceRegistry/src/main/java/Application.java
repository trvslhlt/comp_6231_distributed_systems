import java.io.IOException;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cluster.management.LeaderElection;
import cluster.management.ServiceRegistry;

public class Application implements Watcher {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int DEFAULT_PORT = 8080;
    private static final int SESSION_TIMEOUT = 3000;
    private ZooKeeper zooKeeper;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        int currentServerPort = args.length == 1 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Application application = new Application();
        ZooKeeper zooKeeper = application.connectToZooKeeper();

        ServiceRegistry serviceRegistry = new ServiceRegistry(zooKeeper);

        OnElectionAction onElectionAction = new OnElectionAction(serviceRegistry, currentServerPort);

        LeaderElection leaderElection = new LeaderElection(zooKeeper, onElectionAction);
        leaderElection.volunteerForLeadership();
        leaderElection.electLeader();

        application.run();
        application.close();
        logger.info("Exiting application");
    }

    public ZooKeeper connectToZooKeeper() throws IOException{
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
        return this.zooKeeper;
    }

    public void run() throws InterruptedException {
        synchronized(this.zooKeeper) {
            this.zooKeeper.wait();
        }
    }

    public void close() throws InterruptedException {
        this.zooKeeper.close();
    }

    // Watcher
    @Override
    public void process(WatchedEvent event) {
        switch (event.getType()) {
            case None:
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    logger.info("Successfully connected to ZooKeeper");
                } else {
                    logger.info("Disconnected from ZooKeeper");
                    this.zooKeeper.notifyAll();
                }
                break;
            default:
                logger.error("unhandled event in application.process: " + event);
        }
    }

}
