package com.org.dev.cloud.api_gateway.filter;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

@Slf4j
@Component
public class GlobalRequestFilter implements GlobalFilter, Ordered {

    private final ReactiveOAuth2AuthorizedClientManager manager;

    public GlobalRequestFilter( ReactiveOAuth2AuthorizedClientManager manager) {
        this.manager = manager;
    }

    private static final Set<String> API_PREFIXES = Set.of(
            "/api/",
            "/user/"
    );


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        // ── SKIP NON-API ROUTES ──────────────────────────────────────
        if (!isApiPath(path)) {
            log.debug("GlobalFilter Not triggered for: " + exchange.getRequest().getPath());
            return chain.filter(exchange);
        }
        // ── TOKEN RELAY FOR API ROUTES ───────────────────────────────
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(context -> {
                   if(context.getAuthentication() instanceof OAuth2AuthenticationToken auth && auth.getPrincipal() instanceof OidcUser oidcUser){
                       log.debug("ID Token: {}", oidcUser.getIdToken().getTokenValue());
                       OAuth2AuthorizeRequest request =
                               OAuth2AuthorizeRequest
                                       .withClientRegistrationId(
                                               auth.getAuthorizedClientRegistrationId())
                                       .principal(auth)
                                       .build();
                       return manager.authorize(request);
                   }
                    return Mono.empty();
                })
                .map(OAuth2AuthorizedClient::getAccessToken)
                .map(OAuth2AccessToken::getTokenValue)
                .flatMap(token -> {
                          ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(r -> r.headers(headers ->
                                headers.setBearerAuth(token)
                        ))
                        .build();
                        return chain.filter(mutatedExchange);
                })
                .switchIfEmpty(
                        Mono.defer(() -> chain.filter(exchange))
                )
                .onErrorResume(e -> {
                    // Log the error and continue without token (or handle differently)
                    log.error("Error retrieving token: " + e.getMessage());
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
