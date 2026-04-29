package com.org.dev.cloud.api_gateway.security;

import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class CustomAuthorizationRequestResolver implements ServerOAuth2AuthorizationRequestResolver {

    private final ServerOAuth2AuthorizationRequestResolver defaultResolver;

    public CustomAuthorizationRequestResolver(ReactiveClientRegistrationRepository repo) {
        this.defaultResolver = new DefaultServerOAuth2AuthorizationRequestResolver(repo);
    }

    @Override
    public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange) {
        return defaultResolver.resolve(exchange)
                .map(this::customizeRequest);
    }

    @Override
    public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange, String clientRegistrationId) {
        return defaultResolver.resolve(exchange, clientRegistrationId)
                .map(this::customizeRequest);
    }

    private OAuth2AuthorizationRequest customizeRequest(OAuth2AuthorizationRequest req) {
        if (req == null) {
            return null;
        }

        Map<String, Object> additionalParams = new HashMap<>(req.getAdditionalParameters());
        additionalParams.put(OAuth2ParameterNames.AUDIENCE, "http://localhost:8081");

        return OAuth2AuthorizationRequest.from(req)
                .additionalParameters(additionalParams)
                .build();
    }
}
