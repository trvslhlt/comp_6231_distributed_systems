package vehiclerental.loadbalancer.election;

import io.etcd.jetcd.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vehiclerental.common.election.EtcdLeaderElection;
import vehiclerental.common.k8s.PodRoleLabeler;
import vehiclerental.loadbalancer.config.InstanceIdentity;

/**
 * Updates state when winning or losing a leadership election.
 */
@Configuration
@ConditionalOnProperty(name = "vehicle-rental.etcd.enabled", havingValue = "true")
public class EtcdElectionConfig {

    private static final String ELECTION_NAME = "load-balancer";

    @Bean(destroyMethod = "close")
    public Client etcdClient(@Value("${vehicle-rental.etcd.endpoints}") String endpoints) {
        return Client.builder().endpoints(endpoints.split(",")).build();
    }

    @Bean(destroyMethod = "close")
    public PodRoleLabeler podRoleLabeler() {
        return new PodRoleLabeler();
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    public EtcdLeaderElection leaderElection(
        Client etcdClient, 
        InstanceIdentity identity, 
        LeadershipState leadershipState,
        PodRoleLabeler podRoleLabeler,
        @Value("${vehicle-rental.etcd.lease-ttl-seconds:10}") long ttlSeconds
    ) {
        return new EtcdLeaderElection(etcdClient, ELECTION_NAME, identity.getInstanceId(), ttlSeconds, new EtcdLeaderElection.LeadershipListener() {
            @Override
            public void onElected() {
                leadershipState.setLeader(true);
                podRoleLabeler.setRole(PodRoleLabeler.ROLE_ACTIVE);
            }

            @Override
            public void onDemoted() {
                leadershipState.setLeader(false);
                podRoleLabeler.setRole(PodRoleLabeler.ROLE_PASSIVE);
            }
        });
    }
}
