package vehiclerental.common.k8s;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Patches a pod's own "role" label so a k8s service selecting 'role=active' only ever
 * routes to the current leader.
 */
public class PodRoleLabeler implements AutoCloseable {

    public static final String ROLE_LABEL = "role";
    public static final String ROLE_ACTIVE = "active";
    public static final String ROLE_PASSIVE = "passive";

    private static final Logger log = LoggerFactory.getLogger(PodRoleLabeler.class);

    private final String podName = System.getenv("POD_NAME");
    private final String namespace = System.getenv("POD_NAMESPACE");
    private KubernetesClient client;

    private boolean runningInKubernetes() {
        return podName != null && namespace != null;
    }

    private static final int MAX_ATTEMPTS = 5;

    public void setRole(String role) {
        if (!runningInKubernetes()) {
            log.info("POD_NAME/POD_NAMESPACE not set (not running in Kubernetes); skipping pod label update to role={}", role);
            return;
        }
        // .edit() does a fresh GET-then-PATCH, so a 409 (another writer touched the pod's
        // resourceVersion between our GET and PATCH, e.g. a kubelet status update racing this
        // right after pod startup) is transient — retrying with the freshly re-read resource
        // version resolves it, rather than leaving the pod permanently mislabeled.
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                client().pods().inNamespace(namespace).withName(podName).edit(pod -> {
                    if (pod.getMetadata().getLabels() == null) {
                        pod.getMetadata().setLabels(new HashMap<>());
                    }
                    pod.getMetadata().getLabels().put(ROLE_LABEL, role);
                    return pod;
                });
                log.info("Set pod {}/{} label {}={}", namespace, podName, ROLE_LABEL, role);
                return;
            } catch (Exception e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("Failed to update pod label to role={} after {} attempts", role, MAX_ATTEMPTS, e);
                } else {
                    log.debug("Attempt {}/{} to update pod label to role={} failed, retrying", attempt, MAX_ATTEMPTS, role, e);
                }
            }
        }
    }

    private synchronized KubernetesClient client() {
        if (client == null) {
            client = new KubernetesClientBuilder().build();
        }
        return client;
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
