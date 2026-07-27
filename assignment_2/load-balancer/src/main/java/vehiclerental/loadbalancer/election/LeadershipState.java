package vehiclerental.loadbalancer.election;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Shared active/passive flag consulted by {@link vehiclerental.loadbalancer.web.LeadershipGuardFilter}.
 * In single-instance dev mode (etcd disabled) this instance is always "active" — there is no
 * one to fail over to. With etcd enabled, {@link EtcdElectionConfig} drives this from the
 * election's onElected/onDemoted callbacks.
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
