package vehiclerental.common.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static vehiclerental.common.EtcdKeys.servicePrefix;

/**
 * Watches service keys in etcd and keeps an in-memory view of the current instances.
 * Entries disappear automatically when a lease expires (no polling health checks).
 */
public class EtcdServiceDiscovery implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(EtcdServiceDiscovery.class);

    private final Client client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String serviceName;
    private final Map<String, ServiceInstance> instances = new ConcurrentHashMap<>();
    private final AtomicInteger roundRobinCounter = new AtomicInteger();

    private Watch.Watcher watcher;

    public EtcdServiceDiscovery(Client client, String serviceName) {
        this.client = client;
        this.serviceName = serviceName;
    }

    /** Starts watching etcd for changes to the service's instances. */
    public void start() {
        ByteSequence prefix = ByteSequence.from(servicePrefix(serviceName), StandardCharsets.UTF_8);
        try {
            client.getKVClient().get(prefix, GetOption.builder().isPrefix(true).build())
                    .get()
                    .getKvs()
                    .forEach(this::upsert);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load initial " + serviceName + " instances from etcd", e);
        }

        watcher = client.getWatchClient().watch(prefix, WatchOption.builder().isPrefix(true).build(), new Watch.Listener() {
            @Override
            public void onNext(WatchResponse response) {
                for (WatchEvent event : response.getEvents()) {
                    if (event.getEventType() == WatchEvent.EventType.DELETE) {
                        remove(event.getKeyValue());
                    } else {
                        upsert(event.getKeyValue());
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.warn("Watch on {} instances failed", serviceName, throwable);
            }

            @Override
            public void onCompleted() {
            }
        });

        log.info("Watching {} for {} instances (starting with {})", servicePrefix(serviceName), serviceName, instances.size());
    }

    /** Adds or updates an instance in the in-memory view. */
    private void upsert(KeyValue kv) {
        try {
            ServiceInstance instance = mapper.readValue(kv.getValue().toString(StandardCharsets.UTF_8), ServiceInstance.class);
            instances.put(instance.instanceId(), instance);
            log.info("{} instance available: {}", serviceName, instance);
        } catch (Exception e) {
            log.warn("Could not parse {} registration at {}", serviceName, kv.getKey().toString(StandardCharsets.UTF_8), e);
        }
    }

    /** Removes an instance from the in-memory view. */
    private void remove(KeyValue kv) {
        String key = kv.getKey().toString(StandardCharsets.UTF_8);
        String instanceId = key.substring(key.lastIndexOf('/') + 1);
        ServiceInstance removed = instances.remove(instanceId);
        if (removed != null) {
            log.info("{} instance no longer available: {}", serviceName, removed);
        }
    }

    /** Returns a snapshot of the current instances. */
    public List<ServiceInstance> currentInstances() {
        return new ArrayList<>(instances.values());
    }

    /** Picks the next instance in round-robin order; empty if none are currently registered. */
    public Optional<ServiceInstance> next() {
        List<ServiceInstance> snapshot = currentInstances();
        if (snapshot.isEmpty()) {
            return Optional.empty();
        }
        int index = Math.floorMod(roundRobinCounter.getAndIncrement(), snapshot.size());
        return Optional.of(snapshot.get(index));
    }

    /** Closes the watcher and stops watching for changes. */
    @Override
    public void close() {
        if (watcher != null) {
            watcher.close();
        }
    }
}
