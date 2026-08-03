package vehiclerental.loadbalancer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vehiclerental.common.config.InstanceIdentity;

@Configuration
public class InstanceIdentityConfig {

    @Bean
    public InstanceIdentity instanceIdentity(
        @Value("${vehicle-rental.instance-id}") String instanceId,
        @Value("${server.port}") int port
    ) {
        return new InstanceIdentity(instanceId, port);
    }
}
