package vehiclerental.loadbalancer.discovery;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;
import vehiclerental.common.discovery.EtcdServiceDiscovery;

import java.util.List;
import java.util.stream.Collectors;

/** Feeds Spring Cloud LoadBalancer with instances discovered via etcd (mirrors Service A's
 * equivalent supplier for Service B — see the module READMEs for why this isn't shared code). */
public class EtcdServiceInstanceListSupplier implements ServiceInstanceListSupplier {

    private final String serviceId;
    private final EtcdServiceDiscovery discovery;

    public EtcdServiceInstanceListSupplier(String serviceId, EtcdServiceDiscovery discovery) {
        this.serviceId = serviceId;
        this.discovery = discovery;
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
        List<ServiceInstance> instances = discovery.currentInstances().stream()
                .map(si -> (ServiceInstance) new DefaultServiceInstance(si.instanceId(), serviceId, si.host(), si.port(), false))
                .collect(Collectors.toList());
        return Flux.just(instances);
    }
}
