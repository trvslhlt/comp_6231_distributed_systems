package vehiclerental.common;

/** Definition of etcd keys used in the system. */
public final class EtcdKeys {

    private EtcdKeys() {}

    public static String servicePrefix(String serviceName) {
        return "/services/" + serviceName + "/";
    }

    public static String serviceInstanceKey(String serviceName, String instanceId) {
        return servicePrefix(serviceName) + instanceId;
    }

    public static String electionPrefix(String electionName) {
        return "/election/" + electionName + "/candidates/";
    }

    public static String electionCandidateKey(String electionName, String candidateId) {
        return electionPrefix(electionName) + candidateId;
    }
}
