package vehiclerental.totalprice.discovery;

import io.etcd.jetcd.Client;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import vehiclerental.common.discovery.EtcdServiceDiscovery;

/**
 * Per Spring Cloud LoadBalancer convention, classes referenced via
 * {@code @LoadBalancerClient(configuration = ...)} must NOT be annotated with
 * {@code @Configuration} — otherwise they would be picked up by the main application context
 * and applied globally instead of scoped to this one named client.
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
