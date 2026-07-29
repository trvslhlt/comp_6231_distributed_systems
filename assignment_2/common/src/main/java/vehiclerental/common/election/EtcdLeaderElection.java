package vehiclerental.common.election;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.TxnResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.op.Cmp;
import io.etcd.jetcd.op.CmpTarget;
import io.etcd.jetcd.op.Op;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.support.CloseableClient;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static vehiclerental.common.EtcdKeys.electionKey;

/**
 * Single-key mutex leader election over etcd: whichever candidate first creates the election
 * key (via a create-if-absent transaction) backed by its own TTL lease is the leader. If its
 * lease ever stops being renewed (process death, network partition), etcd deletes the key,
 * the losing candidates notice the deletion and race to create it again.
 *
 * This intentionally only ever has two candidates in this project (the two load-balancer
 * replicas), so a simple mutex is sufficient — etcd's own fair/FIFO election recipe is not
 * needed here.
 */
public class EtcdLeaderElection implements AutoCloseable {

    public interface LeadershipListener {
        void onElected();

        void onDemoted();
    }

    private static final Logger log = LoggerFactory.getLogger(EtcdLeaderElection.class);
    private static final long RETRY_BACKOFF_SECONDS = 2;

    private final Client client;
    private final ByteSequence keyBytes;
    private final ByteSequence candidateIdBytes;
    private final String candidateId;
    private final long ttlSeconds;
    private final LeadershipListener listener;
    private final ExecutorService executor;

    private volatile boolean running = false;

    public EtcdLeaderElection(Client client, String electionName, String candidateId, long ttlSeconds, LeadershipListener listener) {
        this.client = client;
        this.keyBytes = ByteSequence.from(electionKey(electionName), StandardCharsets.UTF_8);
        this.candidateId = candidateId;
        this.candidateIdBytes = ByteSequence.from(candidateId, StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
        this.listener = listener;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "etcd-leader-election");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        running = true;
        executor.submit(this::campaignLoop);
    }

    private void campaignLoop() {
        while (running) {
            try {
                long leaseId = client.getLeaseClient().grant(ttlSeconds).get().getID();

                Cmp keyAbsent = new Cmp(keyBytes, Cmp.Op.EQUAL, CmpTarget.createRevision(0));
                Op claimKey = Op.put(keyBytes, candidateIdBytes, PutOption.builder().withLeaseId(leaseId).build());
                Op readKey = Op.get(keyBytes, GetOption.DEFAULT);

                TxnResponse txnResponse = client.getKVClient().txn().If(keyAbsent).Then(claimKey).Else(readKey).commit().get();

                if (txnResponse.isSucceeded()) {
                    holdLeadershipUntilLost(leaseId);
                } else {
                    client.getLeaseClient().revoke(leaseId).get();
                    waitForCurrentLeaderToDisappear();
                }
            } catch (Exception e) {
                log.warn("Election attempt for {} failed, retrying in {}s", candidateId, RETRY_BACKOFF_SECONDS, e);
                sleep(RETRY_BACKOFF_SECONDS);
            }
        }
    }

    private void holdLeadershipUntilLost(long leaseId) {
        CountDownLatch lostLeadership = new CountDownLatch(1);
        CloseableClient keepAlive = client.getLeaseClient().keepAlive(leaseId, new StreamObserver<>() {
            @Override
            public void onNext(LeaseKeepAliveResponse value) {
                // lease renewed, still leader
            }

            @Override
            public void onError(Throwable t) {
                log.warn("Lost leadership: lease keep-alive for {} failed", candidateId, t);
                lostLeadership.countDown();
            }

            @Override
            public void onCompleted() {
                lostLeadership.countDown();
            }
        });

        log.info("{} elected leader", candidateId);
        listener.onElected();
        try {
            lostLeadership.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            keepAlive.close();
            listener.onDemoted();
            log.info("{} is no longer leader", candidateId);
        }
    }

    private void waitForCurrentLeaderToDisappear() throws InterruptedException {
        CountDownLatch leaderGone = new CountDownLatch(1);
        Watch.Watcher watcher = client.getWatchClient().watch(keyBytes, WatchOption.DEFAULT, new Watch.Listener() {
            @Override
            public void onNext(WatchResponse response) {
                for (WatchEvent event : response.getEvents()) {
                    if (event.getEventType() == WatchEvent.EventType.DELETE) {
                        leaderGone.countDown();
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                leaderGone.countDown();
            }

            @Override
            public void onCompleted() {
            }
        });

        try {
            leaderGone.await();
        } finally {
            watcher.close();
        }
    }

    private void sleep(long seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        running = false;
        executor.shutdownNow();
    }
}
