package com.org.dev.cloud.useraccount.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.org.dev.cloud.useraccount.exception.UserAccountExceptions.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception handler.
 * Returns RFC 7807 ProblemDetail responses — structured, machine-readable errors.
 * Never leaks stack traces or internal details to the client.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public Mono<ProblemDetail> handleNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return Mono.just(problem(HttpStatus.NOT_FOUND, "User Not Found", ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public Mono<ProblemDetail> handleConflict(UserAlreadyExistsException ex) {
        log.warn("Registration conflict: {}", ex.getMessage());
        return Mono.just(problem(HttpStatus.CONFLICT, "User Already Exists", ex.getMessage()));
    }

    @ExceptionHandler(AccountDeletedException.class)
    public Mono<ProblemDetail> handleDeleted(AccountDeletedException ex) {
        log.warn("Deleted account access attempt: {}", ex.getMessage());
        return Mono.just(problem(HttpStatus.GONE, "Account Deleted", ex.getMessage()));
    }

    @ExceptionHandler(AccountSuspendedException.class)
    public Mono<ProblemDetail> handleSuspended(AccountSuspendedException ex) {
        log.warn("Suspended account access attempt: {}", ex.getMessage());
        return Mono.just(problem(HttpStatus.FORBIDDEN, "Account Suspended", ex.getMessage()));
    }

    /** Handles @Valid / @Validated constraint violations */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ProblemDetail> handleValidation(WebExchangeBindException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.debug("Validation failed: {}", errors);
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Validation Failed", errors);
        pd.setProperty("fields", ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage(),
                        (a, b) -> a
                )));
        return Mono.just(pd);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ProblemDetail> handleGeneric(Exception ex) {
        // Log the full exception internally but never expose it externally
        log.error("Unhandled exception", ex);
        return Mono.just(problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later."));
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("https://api.example.com/errors/" + title.toLowerCase().replace(" ", "-")));
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}
