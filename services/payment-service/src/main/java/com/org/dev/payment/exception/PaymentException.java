package com.org.dev.payment.exception;

import com.org.dev.payment.dto.PaymentDTOs.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

public class PaymentException extends RuntimeException {
    public PaymentException(String message) { super(message); }

    public static class NotFoundException extends PaymentException {
        public NotFoundException(String m) { super(m); }
    }
    public static class SignatureVerificationException extends PaymentException {
        public SignatureVerificationException(String m) { super(m); }
    }
    public static class ProviderException extends PaymentException {
        public ProviderException(String m) { super(m); }
    }
    public static class InvalidStateException extends PaymentException {
        public InvalidStateException(String m) { super(m); }
    }
    public static class UnauthorizedException extends PaymentException {
        public UnauthorizedException(String m) { super(m); }
    }
    public static class ConfigurationException extends PaymentException {
        public ConfigurationException(String m) { super(m); }
    }

    @Provider
    public static class GlobalExceptionMapper implements ExceptionMapper<Exception> {
        @Override
        public Response toResponse(Exception e) {
            if (e instanceof NotFoundException ex)
                return Response.status(404).entity(new ErrorResponse("not_found", ex.getMessage(), 404)).build();
            if (e instanceof SignatureVerificationException ex)
                return Response.status(400).entity(new ErrorResponse("signature_invalid", ex.getMessage(), 400)).build();
            if (e instanceof UnauthorizedException ex)
                return Response.status(403).entity(new ErrorResponse("forbidden", ex.getMessage(), 403)).build();
            if (e instanceof InvalidStateException ex)
                return Response.status(409).entity(new ErrorResponse("invalid_state", ex.getMessage(), 409)).build();
            if (e instanceof ProviderException ex)
                return Response.status(502).entity(new ErrorResponse("provider_error", ex.getMessage(), 502)).build();

            return Response.status(500)
                    .entity(new ErrorResponse("internal_error", e.getMessage(), 500)).build();
        }
    }
}
