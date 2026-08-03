package vehiclerental.loadbalancer.election;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Shared active/passive flag.
 * In single-instance dev mode this instance is always "active".
 */
@Component
public class LeadershipState {

    private volatile boolean leader;

    public LeadershipState(@Value("${vehicle-rental.etcd.enabled:false}") boolean etcdEnabled) {
        this.leader = !etcdEnabled;
    }

    public boolean isLeader() {
        return leader;
    }

    public void setLeader(boolean leader) {
        this.leader = leader;
    }
}
