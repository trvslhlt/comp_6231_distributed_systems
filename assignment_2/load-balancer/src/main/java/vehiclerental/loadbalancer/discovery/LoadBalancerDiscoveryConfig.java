package vehiclerental.loadbalancer.discovery;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Spring Cloud LoadBalancer to resolve "http://service-vehicle-total-price"
 * against instances of total price API discovered via etcd.
 * Active only when vehicle-rental.etcd.enabled=true.
 * Body is empty — this class exists only to carry the annotations below.
 */
@Configuration
@ConditionalOnProperty(name = "vehicle-rental.etcd.enabled", havingValue = "true")
@LoadBalancerClient(name = TotalPriceLoadBalancerConfiguration.SERVICE_ID, configuration = TotalPriceLoadBalancerConfiguration.class)
public class LoadBalancerDiscoveryConfig {}
