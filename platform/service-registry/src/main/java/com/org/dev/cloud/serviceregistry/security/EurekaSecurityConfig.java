package com.org.dev.cloud.serviceregistry.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Slf4j
@Configuration
@EnableWebSecurity
public class EurekaSecurityConfig {

    @Bean
    public SecurityFilterChain eurekaSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Allow registration, renewal, cancellation without authentication
                        .requestMatchers("/eureka/apps/**").permitAll()
                        .requestMatchers("/eureka/v2/apps/**").permitAll()   // sometimes used
                        // Kubernetes probes (no auth)
                        .requestMatchers(
                                "/actuator/health/**"
                        ).permitAll()
                        // Eureka dashboard static resources (no auth for assets)
                        .requestMatchers(
                                "/",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()
                        // Allow Eureka dashboard + other endpoints to require login
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .formLogin(Customizer.withDefaults())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/eureka/**"));// Disable CSRF for registration

        return http.build();
    }
}
