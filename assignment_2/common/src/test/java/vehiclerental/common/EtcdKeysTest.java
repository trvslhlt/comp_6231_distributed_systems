package vehiclerental.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EtcdKeysTest {

    @Test
    void servicePrefixEndsWithTrailingSlash() {
        assertThat(EtcdKeys.servicePrefix("service-vehicle-season-price"))
                .isEqualTo("/services/service-vehicle-season-price/");
    }

    @Test
    void serviceInstanceKeyAppendsInstanceIdToPrefix() {
        assertThat(EtcdKeys.serviceInstanceKey("service-vehicle-season-price", "season-price-1"))
                .isEqualTo("/services/service-vehicle-season-price/season-price-1");
    }

    @Test
    void electionPrefixEndsWithCandidatesSegment() {
        assertThat(EtcdKeys.electionPrefix("load-balancer"))
                .isEqualTo("/election/load-balancer/candidates/");
    }

    @Test
    void electionCandidateKeyAppendsCandidateIdToPrefix() {
        assertThat(EtcdKeys.electionCandidateKey("load-balancer", "lb-1"))
                .isEqualTo("/election/load-balancer/candidates/lb-1");
    }

    @Test
    void differentServicesGetDistinctPrefixes() {
        assertThat(EtcdKeys.servicePrefix("service-vehicle-season-price"))
                .isNotEqualTo(EtcdKeys.servicePrefix("service-vehicle-total-price"));
    }
}
