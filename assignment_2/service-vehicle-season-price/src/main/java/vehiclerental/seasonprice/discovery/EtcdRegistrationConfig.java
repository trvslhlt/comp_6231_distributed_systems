package vehiclerental.seasonprice.discovery;

import io.etcd.jetcd.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vehiclerental.common.discovery.EtcdServiceRegistry;
import vehiclerental.common.discovery.HostResolver;
import vehiclerental.common.discovery.ServiceInstance;
import vehiclerental.common.config.InstanceIdentity;

import java.net.UnknownHostException;

/** 
 * Registers this instance in etcd for discovery.
 * Active only when {@code vehicle-rental.etcd.enabled=true}. 
 */
@Configuration
@ConditionalOnProperty(name = "vehicle-rental.etcd.enabled", havingValue = "true")
public class EtcdRegistrationConfig {

    public static final String SERVICE_ID = "service-vehicle-season-price";

    @Bean(destroyMethod = "close")
    public Client etcdClient(@Value("${vehicle-rental.etcd.endpoints}") String endpoints) {
        return Client.builder().endpoints(endpoints.split(",")).build();
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    public EtcdServiceRegistry selfRegistration(
        Client etcdClient, 
        InstanceIdentity identity,
        @Value("${vehicle-rental.etcd.lease-ttl-seconds:10}") long ttlSeconds
    ) throws UnknownHostException {
        ServiceInstance self = new ServiceInstance(
            identity.getInstanceId(),
            HostResolver.resolveHost(),
            identity.getPort());
        return new EtcdServiceRegistry(etcdClient, SERVICE_ID, self, ttlSeconds);
    }
}
