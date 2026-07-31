package vehiclerental.loadbalancer.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import vehiclerental.loadbalancer.election.LeadershipState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// GatewayFilterChain is an interface, so Mockito's proxy-based mocking works here without
// hitting the concrete-class/bytebuddy JDK limitation noted in SeasonPriceClientTest.
@ExtendWith(MockitoExtension.class)
class LeadershipGuardFilterTest {

    @Mock
    private GatewayFilterChain chain;

    @Test
    void leaderRequestsAreForwardedToTheChain() {
        LeadershipState leadershipState = new LeadershipState(true);
        leadershipState.setLeader(true);
        LeadershipGuardFilter filter = new LeadershipGuardFilter(leadershipState);
        ServerWebExchange exchange = exchange();
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void nonLeaderRequestsAreRejectedWith503WithoutReachingTheChain() {
        LeadershipState leadershipState = new LeadershipState(true);
        // leadershipState.setLeader(true) deliberately not called — starts false, matching
        // "hasn't won an election yet" / "just been demoted" in etcd-enabled mode.
        LeadershipGuardFilter filter = new LeadershipGuardFilter(leadershipState);
        ServerWebExchange exchange = exchange();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verify(chain, never()).filter(exchange);
    }

    private static ServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/total").build());
    }
}
