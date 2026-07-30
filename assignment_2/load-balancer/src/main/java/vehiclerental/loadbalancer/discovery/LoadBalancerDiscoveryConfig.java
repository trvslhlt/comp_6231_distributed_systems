package vehiclerental.loadbalancer.discovery;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the "service-vehicle-total-price" load-balanced client so Gateway routes can use
 * uri("lb://service-vehicle-total-price"). The shared etcd {@code Client} bean this depends on
 * is defined once, in {@link vehiclerental.loadbalancer.election.EtcdElectionConfig}, and
 * resolved here from the parent application context.
 */
@Configuration
@ConditionalOnProperty(name = "vehicle-rental.etcd.enabled", havingValue = "true")
@LoadBalancerClient(name = TotalPriceLoadBalancerConfiguration.SERVICE_ID, configuration = TotalPriceLoadBalancerConfiguration.class)
public class LoadBalancerDiscoveryConfig {}
