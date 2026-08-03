package vehiclerental.common.config;

/**
 * Represents the identity of this service instance. Used in service discovery.
 * Not a {@code @Component}: classes in {@code common} aren't picked up by any app's
 * component scan (each {@code @SpringBootApplication} only scans its own package tree), so
 * this is a plain value object — each module provides its own {@code @Bean} factory for it.
 */
public class InstanceIdentity {

    private final String instanceId;
    private final int port;

    public InstanceIdentity(String instanceId, int port) {
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
