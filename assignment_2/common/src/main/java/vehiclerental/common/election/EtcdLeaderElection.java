package vehiclerental.common.election;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static vehiclerental.common.EtcdKeys.electionCandidateKey;
import static vehiclerental.common.EtcdKeys.electionPrefix;

/**
 * Fair/FIFO leader election over etcd.
 */
public class EtcdLeaderElection implements AutoCloseable {

    public interface LeadershipListener {
        void onElected();
        void onDemoted();
    }

    private static final Logger log = LoggerFactory.getLogger(EtcdLeaderElection.class);
    private static final long RETRY_BACKOFF_SECONDS = 2;
    private static final long WAKE_POLL_SECONDS = 1;

    private final Client client;
    private final ByteSequence prefixBytes;
    private final ByteSequence ownKeyBytes;
    private final ByteSequence candidateIdBytes;
    private final String candidateId;
    private final long ttlSeconds;
    private final LeadershipListener listener;
    private final ExecutorService executor;

    private volatile boolean running = false;

    public EtcdLeaderElection(
        Client client,
        String electionName,
        String candidateId,
        long ttlSeconds,
        LeadershipListener listener
    ) {
        this.client = client;
        this.prefixBytes = ByteSequence.from(electionPrefix(electionName), StandardCharsets.UTF_8);
        this.ownKeyBytes = ByteSequence.from(electionCandidateKey(electionName, candidateId), StandardCharsets.UTF_8);
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

    /**
     * Starts the election campaign in a background thread. The caller should call {@link #close()} to stop
     * the campaign when it's no longer needed.
     */
    public void start() {
        running = true;
        executor.submit(this::campaignLoop);
    }

    /**
     * Repeatedly attempts to run a single election term, sleeping and retrying on failure.
     */
    private void campaignLoop() {
        while (running) {
            try {
                runOneTerm();
            } catch (Exception e) {
                log.warn("Election attempt for {} failed, retrying in {}s", candidateId, RETRY_BACKOFF_SECONDS, e);
                sleep(RETRY_BACKOFF_SECONDS);
            }
        }
    }

    /**
     * Registers this candidate's own key, then either leads (if it's first in line) or waits in
     * line, holding leadership (if won) until the lease is lost.
     */
    private void runOneTerm() throws Exception {
        long leaseId = client.getLeaseClient().grant(ttlSeconds).get().getID();
        client.getKVClient().put(ownKeyBytes, candidateIdBytes, PutOption.builder().withLeaseId(leaseId).build()).get();

        AtomicBoolean leaseLost = new AtomicBoolean(false);
        // Points at whichever latch the campaign thread is currently blocked on, so the
        // keep-alive callback (running on a gRPC thread) can wake it up immediately on failure
        // instead of only being noticed on the next poll.
        AtomicReference<CountDownLatch> currentWait = new AtomicReference<>();
        CloseableClient keepAlive = client.getLeaseClient().keepAlive(leaseId, new StreamObserver<>() {
            @Override
            public void onNext(LeaseKeepAliveResponse value) {
                // lease renewed, still holding our place (or leadership)
            }

            @Override
            public void onError(Throwable t) {
                log.warn("Lease for {} lost: keep-alive failed", candidateId, t);
                wakeUp();
            }

            @Override
            public void onCompleted() {
                wakeUp();
            }

            private void wakeUp() {
                leaseLost.set(true);
                CountDownLatch latch = currentWait.get();
                if (latch != null) {
                    latch.countDown();
                }
            }
        });

        // Set the instant onElected() fires (not derived from waitInLineThenHoldLeadership's
        // return value) so a close()-triggered interrupt during holdLeadershipUntilLost still
        // results in onDemoted() being called from the finally block below.
        AtomicBoolean elected = new AtomicBoolean(false);
        try {
            waitInLineThenHoldLeadership(leaseLost, elected, currentWait);
        } finally {
            keepAlive.close();
            if (elected.get()) {
                listener.onDemoted();
                log.info("{} is no longer leader", candidateId);
            }
        }
    }

    /**
     * Repeatedly checks this candidate's position among all live candidate keys. If it's first,
     * announces leadership (setting {@code elected}) and blocks until the lease is lost.
     * Otherwise, watches only the one key directly ahead of it and re-checks position once that
     * key disappears.
     */
    private void waitInLineThenHoldLeadership(AtomicBoolean leaseLost, AtomicBoolean elected, AtomicReference<CountDownLatch> currentWait)
            throws Exception {
        while (running && !leaseLost.get()) {
            List<KeyValue> candidates = client.getKVClient().get(prefixBytes, GetOption.builder()
                    .isPrefix(true)
                    .withSortField(GetOption.SortTarget.CREATE)
                    .withSortOrder(GetOption.SortOrder.ASCEND)
                    .build()).get().getKvs();

            int position = indexOfOwnKey(candidates);
            if (position < 0) {
                // Our own key is gone — our lease already expired or was revoked.
                return;
            }

            if (position == 0) {
                log.info("{} elected leader", candidateId);
                listener.onElected();
                elected.set(true);
                awaitLatch(new CountDownLatch(1), leaseLost, currentWait);
                return;
            }

            ByteSequence predecessorKey = candidates.get(position - 1).getKey();
            waitForKeyToDisappear(predecessorKey, leaseLost, currentWait);
        }
    }

    private int indexOfOwnKey(List<KeyValue> candidates) {
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).getKey().equals(ownKeyBytes)) {
                return i;
            }
        }
        return -1;
    }

    private void waitForKeyToDisappear(ByteSequence key, AtomicBoolean leaseLost, AtomicReference<CountDownLatch> currentWait) {
        CountDownLatch keyGoneOrLeaseLost = new CountDownLatch(1);
        Watch.Watcher watcher = client.getWatchClient().watch(key, WatchOption.DEFAULT, new Watch.Listener() {
            @Override
            public void onNext(WatchResponse response) {
                for (WatchEvent event : response.getEvents()) {
                    if (event.getEventType() == WatchEvent.EventType.DELETE) {
                        keyGoneOrLeaseLost.countDown();
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                keyGoneOrLeaseLost.countDown();
            }

            @Override
            public void onCompleted() {
            }
        });

        try {
            awaitLatch(keyGoneOrLeaseLost, leaseLost, currentWait);
        } finally {
            watcher.close();
        }
    }

    /**
     * Blocks on {@code latch}, registering it as the thing {@code leaseLost} should wake, with a
     * short poll as a backstop in case the lease was lost in the narrow window before
     * registration. Swallows interruption (restoring the interrupt flag) rather than throwing,
     * so a {@link #close()} during this wait unwinds cleanly through the normal return path —
     * the caller's {@code finally} blocks (closing the watcher, calling onDemoted) still run.
     */
    private void awaitLatch(CountDownLatch latch, AtomicBoolean leaseLost, AtomicReference<CountDownLatch> currentWait) {
        currentWait.set(latch);
        try {
            while (!leaseLost.get() && latch.getCount() > 0) {
                latch.await(WAKE_POLL_SECONDS, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
