package vehiclerental.totalprice.discovery;

import io.etcd.jetcd.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import vehiclerental.common.discovery.EtcdServiceRegistry;
import vehiclerental.common.discovery.HostResolver;
import vehiclerental.common.discovery.ServiceInstance;
import vehiclerental.common.config.InstanceIdentity;

import java.net.UnknownHostException;

/**
 * Registers this instance in etcd under
 * service-vehicle-total-price, and wires Spring Cloud LoadBalancer to resolve
 * "http://service-vehicle-season-price" against instances of Service B discovered via etcd.
 * Active only when vehicle-rental.etcd.enabled=true. 
 */
@Configuration
@ConditionalOnProperty(name = "vehicle-rental.etcd.enabled", havingValue = "true")
@LoadBalancerClient(name = EtcdDiscoveryConfig.SEASON_PRICE_SERVICE_ID, configuration = SeasonPriceLoadBalancerConfiguration.class)
public class EtcdDiscoveryConfig {

    public static final String SEASON_PRICE_SERVICE_ID = "service-vehicle-season-price";
    public static final String THIS_SERVICE_ID = "service-vehicle-total-price";

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
            identity.getPort()
        );
        return new EtcdServiceRegistry(etcdClient, THIS_SERVICE_ID, self, ttlSeconds);
    }

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient seasonPriceRestClient(RestClient.Builder loadBalancedRestClientBuilder) {
        return loadBalancedRestClientBuilder.baseUrl("http://" + SEASON_PRICE_SERVICE_ID).build();
    }
}
