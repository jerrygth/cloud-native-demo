package com.org.dev.cloud.api_gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;

import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.savedrequest.ServerRequestCache;
import org.springframework.security.web.server.savedrequest.WebSessionServerRequestCache;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ReactiveClientRegistrationRepository repository) {

        ServerRequestCache requestCache = new WebSessionServerRequestCache();

        RedirectServerAuthenticationSuccessHandler successHandler =
                new RedirectServerAuthenticationSuccessHandler();
        successHandler.setRequestCache(requestCache);

        http
                // ── CORS ───────────────────────────────────────────────
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── CSRF ───────────────────────────────────────────────
                // CookieServerCsrfTokenRepository:
                //   - Sets XSRF-TOKEN cookie (JS-readable, not HttpOnly)
                //   - Expects X-XSRF-TOKEN header on POST/PUT/DELETE
                // withHttpOnlyFalse(): React JS must be able to read the cookie
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler())
                        .requireCsrfProtectionMatcher(exchange -> {
                            String path = exchange.getRequest().getPath().value();
                            HttpMethod method = exchange.getRequest().getMethod();
                            Set<HttpMethod> ALLOWED_METHODS = new HashSet<>(
                                    Arrays.asList(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.TRACE, HttpMethod.OPTIONS));
                            // If it's the webhook, we don't need CSRF
                            if ("/api/payments/webhook".equals(path)) {
                                return MatchResult.notMatch();
                            }

                            // For everything else, follow standard rules:
                            // Only require CSRF for "Unsafe" methods (POST, PUT, DELETE)
                            return ALLOWED_METHODS.contains(method)
                                    ? MatchResult.notMatch()
                                    : MatchResult.match();
                        })

                )

                // ── ROUTE AUTHORIZATION ────────────────────────────────
                .authorizeExchange(exchanges -> exchanges
                        // Allow React app assets and login flow without session
                        .pathMatchers("/login/**", "/oauth2/**").permitAll()
                        // Allow Actuator health check (for Kubernetes probes)
                        .pathMatchers("/actuator/**").permitAll()
                        //Allow calls from razor for webhook events
                        .pathMatchers("/api/payments/webhook").permitAll()
                        // Allow preflight CORS requests (OPTIONS)
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // ADD THIS — browser always fetches these, no need to auth them
                        .pathMatchers("/favicon.ico", "/favicon.png", "/robots.txt").permitAll()
                        // Everything else requires an authenticated session
                        .anyExchange().authenticated()
                )

                // ── OAUTH2 LOGIN ───────────────────────────────────────
                // Gateway is the OAuth2 client — initiates Auth0 login
                .oauth2Login(oauth2 -> oauth2
                        .authenticationSuccessHandler(successHandler)
                        .authorizationRequestResolver(new CustomAuthorizationRequestResolver(repository))
                )

                // ── LOGOUT ─────────────────────────────────────────────
                // Spring Security handles /logout automatically:
                //   1. Invalidates the server-side session
                //   2. Clears the SESSION cookie (Max-Age=0)
                //   3. We redirect to Auth0's /v2/logout to clear SSO session
                .logout(logout -> logout
                        .requiresLogout(ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/logout")) // Set http method to GET for logout endpoint, default POST
                        .logoutSuccessHandler((exchange, authentication) -> {
                            // After local logout, clear Auth0's SSO session too
                            // This prevents auto-login if user visits again
                            String auth0LogoutUrl = "https://dev-cxy2s4rbkgtrmylk.us.auth0.com/v2/logout"
                                    + "?client_id=${AUTH0_CLIENT_ID}"
                                    + "&returnTo=http://localhost:3000/login";
                            exchange.getExchange().getResponse().setStatusCode(HttpStatus.FOUND);
                            exchange.getExchange().getResponse().getHeaders()
                                    .setLocation(URI.create(auth0LogoutUrl));
                            return Mono.empty();
                        })
                );

        return http.build();
    }

    /**
     * CORS Configuration
     * Must use exact origin (not "*") when credentials are included.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Single origin now — everything goes through :8081
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}