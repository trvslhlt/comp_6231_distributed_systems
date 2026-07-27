package vehiclerental.common.k8s;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Patches this pod's own "role" label so a Kubernetes Service selecting role=active only ever
 * routes to the current leader — plain round-robin across a Deployment's pods (what a normal
 * Service does) is not active-passive. Reads its own pod identity from the Downward API
 * (POD_NAME / POD_NAMESPACE env vars, wired up in the load-balancer Deployment manifest).
 * Outside Kubernetes (e.g. docker-compose / local dev) this is a no-op.
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

    public void setRole(String role) {
        if (!runningInKubernetes()) {
            log.info("POD_NAME/POD_NAMESPACE not set (not running in Kubernetes); skipping pod label update to role={}", role);
            return;
        }
        try {
            client().pods().inNamespace(namespace).withName(podName).edit(pod -> {
                if (pod.getMetadata().getLabels() == null) {
                    pod.getMetadata().setLabels(new HashMap<>());
                }
                pod.getMetadata().getLabels().put(ROLE_LABEL, role);
                return pod;
            });
            log.info("Set pod {}/{} label {}={}", namespace, podName, ROLE_LABEL, role);
        } catch (Exception e) {
            log.warn("Failed to update pod label to role={}", role, e);
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
