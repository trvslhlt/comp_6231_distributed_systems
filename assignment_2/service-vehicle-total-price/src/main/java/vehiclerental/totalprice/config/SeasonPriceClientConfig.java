package vehiclerental.totalprice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.Objects;

/**
 * Configures a RestClient for the season price service using a fixed base URL.
 * Active only when vehicle-rental.etcd.enabled=false or not set.
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
