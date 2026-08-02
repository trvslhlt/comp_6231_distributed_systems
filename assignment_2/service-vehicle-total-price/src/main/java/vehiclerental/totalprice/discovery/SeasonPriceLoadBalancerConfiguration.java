package vehiclerental.totalprice.discovery;

import io.etcd.jetcd.Client;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import vehiclerental.common.discovery.EtcdServiceDiscovery;
import vehiclerental.common.discovery.EtcdServiceInstanceListSupplier;

/**
 * Configures Spring Cloud LoadBalancer to resolve "http://service-vehicle-season-price"
 * against instances of season price API discovered via etcd.
 */
public class SeasonPriceLoadBalancerConfiguration {

    @Bean(initMethod = "start", destroyMethod = "close")
    public EtcdServiceDiscovery seasonPriceDiscovery(Client etcdClient) {
        return new EtcdServiceDiscovery(etcdClient, EtcdDiscoveryConfig.SEASON_PRICE_SERVICE_ID);
    }

    @Bean
    public ServiceInstanceListSupplier serviceInstanceListSupplier(EtcdServiceDiscovery seasonPriceDiscovery) {
        return new EtcdServiceInstanceListSupplier(EtcdDiscoveryConfig.SEASON_PRICE_SERVICE_ID, seasonPriceDiscovery);
    }
}
