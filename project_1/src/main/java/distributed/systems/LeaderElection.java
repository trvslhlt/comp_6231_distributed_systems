package distributed.systems;

import java.io.IOException;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;


public class LeaderElection implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000; // time before considering client dead
    private ZooKeeper zooKeeper;

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Running 'main'");
        
        LeaderElection leaderElection = new LeaderElection();
        leaderElection.connectToZooKeeper();
        leaderElection.run();
        leaderElection.close();

        System.out.println("Disconnected from ZooKeeper. Exiting Applicaiton.");
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

    // Watcher
    @Override
    public void process(WatchedEvent event) {
        switch(event.getType()) {
            case None:
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Successfully connected to ZooKeeper");
                } else {
                    synchronized (this.zooKeeper) {
                        System.out.println("Disconnected from ZooKeeper event");
                        this.zooKeeper.notifyAll();
                    }
                }
            default:
                System.out.println("unhandled event");
        }
    }
}
