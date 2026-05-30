package distributed.systems;

import java.io.IOException;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;


public class LeaderElection implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000; // time before considering client dead
    private ZooKeeper zooKeeper;

    public static void main(String[] args) throws IOException {
        System.out.println("running");
        LeaderElection leaderElection = new LeaderElection();
        leaderElection.connectToZooKeeper();

        System.out.println("Press Enter to exit the application...");
        System.in.read(); // This blocks the main thread until you hit Enter
    }

    public void connectToZooKeeper() throws IOException{
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }

    // Watcher
    @Override
    public void process(WatchedEvent event) {
        switch(event.getType()) {
            case None:
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Successfully connected to ZooKeeper");
                }
            default:
                System.out.println("unhandled event");
        }
    }
}
