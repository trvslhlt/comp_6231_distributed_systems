package vehiclerental.totalprice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.Objects;

/**
 * Phase 1 wiring: talk to a single, fixed Service B instance. Active only when
 * vehicle-rental.etcd.enabled=false (the default). When etcd is enabled,
 * {@link vehiclerental.totalprice.discovery.EtcdDiscoveryConfig} supplies the RestClient bean
 * instead, backed by Spring Cloud LoadBalancer over instances discovered via etcd.
 */
@Configuration
public class SeasonPriceClientConfig {

    @Bean
    @ConditionalOnProperty(name = "vehicle-rental.etcd.enabled", havingValue = "false", matchIfMissing = true)
    public RestClient seasonPriceRestClient(@Value("${vehicle-rental.season-price-service.base-url}") String baseUrl) {
        Objects.requireNonNull(baseUrl, "vehicle-rental.season-price-service.base-url must be set");
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
