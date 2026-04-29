package com.org.dev.cloud.api_gateway.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class LandingPageController {

    @GetMapping("/login/welcome")
    public Mono<String> welcome(@AuthenticationPrincipal OAuth2User user) {
        String name = user.getAttribute("name");
        String email = user.getAttribute("email");
        return Mono.just("Welcome, " + name + "! Your email is: " + email);
    }

}
