package com.org.dev.cloud.useraccount.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.session.WebSessionManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchanges -> exchanges
                        // Health/info endpoints: publicly accessible for load balancer probes
                        .pathMatchers("/actuator/health", "/actuator/info","/actuator/prometheus",
                                "/actuator/health/liveness","/actuator/health/readiness").permitAll()

                        // Registration: any authenticated user (first-time sign-up)
                        .pathMatchers(HttpMethod.POST, "/api/v1/users/register").authenticated()

                        // Profile and preferences: require a valid user scope
                        .pathMatchers(HttpMethod.GET, "/api/v1/users/me").hasAuthority("SCOPE_profile:read")
                        .pathMatchers(HttpMethod.PATCH, "/api/v1/users/me").hasAuthority("SCOPE_profile:write")
                        .pathMatchers("/api/v1/users/me/preferences/**").hasAuthority("SCOPE_profile:write")

                        // Account deletion: requires an explicit delete scope
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/users/me").hasAuthority("SCOPE_account:delete")

                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                // Stateless JWT — CSRF protection not applicable
                .csrf(ServerHttpSecurity.CsrfSpec::disable);

        return http.build();
    }


    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String scopeClaim = jwt.getClaimAsString("scope");
            if (scopeClaim == null || scopeClaim.isBlank()) {
                return Flux.empty();
            }
            List<SimpleGrantedAuthority> authorities = List.of(scopeClaim.split(" ")).stream()
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                    .toList();
            log.debug("JWT authorities resolved: {}", authorities);
            return Flux.fromIterable(authorities);
        });
        return converter;
    }

    /** Disable server-side sessions — JWT is the only state carrier */
    @Bean
    public WebSessionManager webSessionManager() {
        return exchange -> Mono.empty();
    }
}