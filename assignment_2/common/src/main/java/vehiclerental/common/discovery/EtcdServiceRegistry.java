package vehiclerental.common.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.support.CloseableClient;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static vehiclerental.common.EtcdKeys.serviceInstanceKey;

/**
 * Registers this process under /services/{serviceName}/{instanceId} in etcd, backed by a
 * TTL lease that is kept alive on a heartbeat. If the process dies, the lease expires and
 * etcd deletes the key on its own — that expiry is what {@link EtcdServiceDiscovery} watches
 * for, so no separate health-check protocol is needed.
 */
public class EtcdServiceRegistry implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(EtcdServiceRegistry.class);

    private final Client client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String serviceName;
    private final ServiceInstance self;
    private final long ttlSeconds;

    private volatile CloseableClient keepAlive;

    public EtcdServiceRegistry(Client client, String serviceName, ServiceInstance self, long ttlSeconds) {
        this.client = client;
        this.serviceName = serviceName;
        this.self = self;
        this.ttlSeconds = ttlSeconds;
    }

    public void start() {
        try {
            LeaseGrantResponse lease = client.getLeaseClient().grant(ttlSeconds).get();
            long leaseId = lease.getID();

            ByteSequence key = ByteSequence.from(serviceInstanceKey(serviceName, self.instanceId()), StandardCharsets.UTF_8);
            ByteSequence value = ByteSequence.from(mapper.writeValueAsString(self), StandardCharsets.UTF_8);

            client.getKVClient().put(key, value, PutOption.newBuilder().withLeaseId(leaseId).build()).get();

            keepAlive = client.getLeaseClient().keepAlive(leaseId, new StreamObserver<>() {
                @Override
                public void onNext(LeaseKeepAliveResponse value) {
                    log.debug("Lease {} for {} renewed, ttl={}s", leaseId, self.instanceId(), value.getTTL());
                }

                @Override
                public void onError(Throwable t) {
                    log.warn("Lease keep-alive for {} failed, registration will expire in etcd", self.instanceId(), t);
                }

                @Override
                public void onCompleted() {
                    // stream closed, typically because close() below was called
                }
            });

            log.info("Registered {} instance {} at {} (lease {}, ttl {}s)", serviceName, self.instanceId(), self.baseUrl(), leaseId, ttlSeconds);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to register " + serviceName + " instance " + self.instanceId() + " in etcd", e);
        }
    }

    @Override
    public void close() {
        if (keepAlive != null) {
            keepAlive.close();
        }
        try {
            client.getKVClient().delete(ByteSequence.from(serviceInstanceKey(serviceName, self.instanceId()), StandardCharsets.UTF_8)).get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Best-effort deregistration of {} did not complete: {}", self.instanceId(), e.toString());
        }
    }
}
