package vehiclerental.totalprice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Represents the identity of this service instance.
 */
@Component
public class InstanceIdentity {

    private final String instanceId;
    private final int port;

    public InstanceIdentity(@Value("${vehicle-rental.instance-id}") String instanceId,
                             @Value("${server.port}") int port) {
        this.instanceId = instanceId;
        this.port = port;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public int getPort() {
        return port;
    }
}
