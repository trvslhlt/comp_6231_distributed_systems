package vehiclerental.loadbalancer.election;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LeadershipStateTest {

    @Test
    void isLeaderByDefaultWhenEtcdDisabled() {
        // Single-instance dev mode: no election happens, so this instance must be the only
        // one ever serving client traffic.
        LeadershipState state = new LeadershipState(false);

        assertThat(state.isLeader()).isTrue();
    }

    @Test
    void isNotLeaderByDefaultWhenEtcdEnabled() {
        // Multi-instance mode: leadership is earned via the etcd election, never assumed.
        LeadershipState state = new LeadershipState(true);

        assertThat(state.isLeader()).isFalse();
    }

    @Test
    void setLeaderOverridesTheInitialValue() {
        LeadershipState state = new LeadershipState(true);

        state.setLeader(true);
        assertThat(state.isLeader()).isTrue();

        state.setLeader(false);
        assertThat(state.isLeader()).isFalse();
    }
}
