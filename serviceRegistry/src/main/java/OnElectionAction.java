import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.zookeeper.KeeperException;
import cluster.management.OnElectionCallback;
import cluster.management.ServiceRegistry;

public class OnElectionAction implements OnElectionCallback {
    private final ServiceRegistry serviceRegistry;
    private final int port;

    public OnElectionAction(ServiceRegistry serviceRegistry, int port) {
        this.serviceRegistry = serviceRegistry;
        this.port = port;
    }

    // OnElectionCallback
    @Override
    public void onElectedToBeLeader() {
        this.serviceRegistry.unreigsterFromCluster();
        this.serviceRegistry.registerForUpdates();
    }

    @Override
    public void onWorker() {
        try {
            String currentServerAddress = String.format(
                "http://%s:%d", 
                InetAddress.getLocalHost().getCanonicalHostName(), 
                port);
            this.serviceRegistry.registerToCluster(currentServerAddress);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

}
