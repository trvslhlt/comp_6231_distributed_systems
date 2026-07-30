package vehiclerental.common.discovery;

/**
 * Represents a service instance in the discovery system.
 */
public record ServiceInstance(String instanceId, String host, int port) {

    public String baseUrl() {
        return "http://" + host + ":" + port;
    }
}
