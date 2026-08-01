package vehiclerental.loadbalancer.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    /** Single-instance dev mode: forward everything to one fixed Service A instance. */
    @Bean
    @ConditionalOnProperty(name = "vehicle-rental.etcd.enabled", havingValue = "false", matchIfMissing = true)
    public RouteLocator fixedRoutes(RouteLocatorBuilder builder,
                                     @Value("${vehicle-rental.total-price-service.base-url}") String baseUrl) {
        return builder.routes()
                .route(
                    "vehicle-total-price", 
                    r -> r.path("/**").uri(baseUrl))
                .build();
    }

    /** Forward to whichever Service A instances etcd currently reports, round-robin. */
    @Bean
    @ConditionalOnProperty(name = "vehicle-rental.etcd.enabled", havingValue = "true")
    public RouteLocator loadBalancedRoutes(RouteLocatorBuilder builder) {
        // The "lb://" prefix tells Spring Cloud Gateway to use the load balancer to resolve the 
        // service name to an actual instance.
        return builder.routes()
                .route(
                    "vehicle-total-price", 
                    r -> r.path("/**").uri("lb://service-vehicle-total-price"))
                .build();
    }
}
