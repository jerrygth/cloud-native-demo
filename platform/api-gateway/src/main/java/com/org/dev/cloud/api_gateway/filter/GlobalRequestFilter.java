package com.org.dev.cloud.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;


@Component
public class GlobalRequestFilter implements GlobalFilter, Ordered {

    private final ReactiveOAuth2AuthorizedClientService clientService;

    public GlobalRequestFilter(ReactiveOAuth2AuthorizedClientService clientService) {
        this.clientService = clientService;
    }

    private static final Set<String> API_PREFIXES = Set.of(
            "/api/",
            "/user/"
    );


    private static final Logger logger = LoggerFactory.getLogger(GlobalRequestFilter.class);
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        // ── SKIP NON-API ROUTES ──────────────────────────────────────
        if (!isApiPath(path)) {
            System.out.println("GlobalFilter Not triggered for: " + exchange.getRequest().getPath());
            return chain.filter(exchange);
        }
        // ── TOKEN RELAY FOR API ROUTES ───────────────────────────────
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(context -> {

                    OAuth2AuthenticationToken auth =
                            (OAuth2AuthenticationToken) context.getAuthentication();

                    DefaultOidcUser user = (DefaultOidcUser) auth.getPrincipal();

                    String idToken = user.getIdToken().getTokenValue();

                    return clientService
                            .loadAuthorizedClient(auth.getAuthorizedClientRegistrationId(), auth.getName())
                            .map(client -> {

                                String accessToken = client.getAccessToken().getTokenValue();

                                logger.debug("Access Token: {}", accessToken);
                                logger.debug("ID Token: {}", idToken);

                                return accessToken;
                            });
                })
                .flatMap(token -> {
                    if (token != null) {
                          ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(r -> r.headers(headers ->
                                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        ))
                        .build();
                        return chain.filter(mutatedExchange);
                    }
                    // If no token, proceed without modification
                    return chain.filter(exchange);
                })
                .switchIfEmpty(
                        Mono.defer(() -> chain.filter(exchange))
                )
                .onErrorResume(e -> {
                    // Log the error and continue without token (or handle differently)
                    logger.error("Error retrieving token: " + e.getMessage());
                    return chain.filter(exchange);
                });
    }

    private boolean isApiPath(String path) {
        for (String prefix : API_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return -200; // Run before most built-in filters (e.g., NettyRoutingFilter)
    }
}
