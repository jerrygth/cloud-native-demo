package com.org.dev.cloud.api_gateway.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.file.AccessDeniedException;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(AccessDeniedException.class)
    Mono<ResponseEntity<ErrorResponse>> handleAccessDeniedException(AccessDeniedException ace, ServerWebExchange ex){

        log.error("Access denied: {}", ace.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("You don't have permission to access this resource")
                .path(ex.getRequest().getPath().toString())
                .build();
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse));
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleAuthenticationNotFound(
            AuthenticationCredentialsNotFoundException ex, ServerWebExchange exchange) {
        log.warn("Authentication required for path: {}", exchange.getRequest().getPath());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Authentication required")
                .path(exchange.getRequest().getPath().toString())
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error));
    }

    @ExceptionHandler(Exception.class)
    Mono<ResponseEntity<String>> handleAll(Exception ex){
        return Mono.just(ResponseEntity.status(500).body("Internal error"));
    }
}
