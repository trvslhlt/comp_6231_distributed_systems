package vehiclerental.common;

/** Central place for the etcd key layout so every module agrees on it. */
public final class EtcdKeys {

    private EtcdKeys() {
    }

    public static String servicePrefix(String serviceName) {
        return "/services/" + serviceName + "/";
    }

    public static String serviceInstanceKey(String serviceName, String instanceId) {
        return servicePrefix(serviceName) + instanceId;
    }

    public static String electionKey(String electionName) {
        return "/election/" + electionName + "/leader";
    }
}
