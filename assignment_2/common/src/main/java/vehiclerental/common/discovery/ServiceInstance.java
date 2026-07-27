package vehiclerental.common.discovery;

public record ServiceInstance(String instanceId, String host, int port) {

    public String baseUrl() {
        return "http://" + host + ":" + port;
    }
}
