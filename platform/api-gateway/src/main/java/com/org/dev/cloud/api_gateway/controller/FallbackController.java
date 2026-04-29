package com.org.dev.cloud.api_gateway.controller;

import com.org.dev.cloud.api_gateway.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user")
    public Mono<ResponseEntity<ErrorResponse>> userFallback() {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("User service is temporarily unavailable. Please try again later.")
                .path("/user")
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error));
    }
}