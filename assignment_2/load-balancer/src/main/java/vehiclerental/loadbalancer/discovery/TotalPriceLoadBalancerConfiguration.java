package vehiclerental.loadbalancer.discovery;

import io.etcd.jetcd.Client;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import vehiclerental.common.discovery.EtcdServiceDiscovery;
import vehiclerental.common.discovery.EtcdServiceInstanceListSupplier;

/**
 * Configures Spring Cloud LoadBalancer to resolve "http://service-vehicle-total-price"
 */
public class TotalPriceLoadBalancerConfiguration {

    public static final String SERVICE_ID = "service-vehicle-total-price";

    @Bean(initMethod = "start", destroyMethod = "close")
    public EtcdServiceDiscovery totalPriceDiscovery(Client etcdClient) {
        return new EtcdServiceDiscovery(etcdClient, SERVICE_ID);
    }

    @Bean
    public ServiceInstanceListSupplier serviceInstanceListSupplier(EtcdServiceDiscovery totalPriceDiscovery) {
        return new EtcdServiceInstanceListSupplier(SERVICE_ID, totalPriceDiscovery);
    }
}
