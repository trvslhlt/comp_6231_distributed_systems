package vehiclerental.loadbalancer.web;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import vehiclerental.loadbalancer.election.LeadershipState;

/**
 * k8s should only ever route to the pod labeled role=active, but label propagation 
 * is not instantaneous. Any request that reaches a non-leader instance
 * is rejected here rather than silently proxied (fail-closed vs. fail-open).
 */
@Component
public class LeadershipGuardFilter implements GlobalFilter, Ordered {

    private final LeadershipState leadershipState;

    public LeadershipGuardFilter(LeadershipState leadershipState) {
        this.leadershipState = leadershipState;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!leadershipState.isLeader()) {
            exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
