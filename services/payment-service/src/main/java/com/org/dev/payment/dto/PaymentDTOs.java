package com.org.dev.payment.dto;

import jakarta.validation.constraints.*;


public class PaymentDTOs {


    public record OrderRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 200)
        String productName,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        Integer amount,

        @Pattern(regexp = "INR|USD|EUR", message = "Unsupported currency")
        String currency,

        @NotBlank
        String receipt
    ) {}


    public record OrderResponse(
        String razorpayOrderId,
        Integer amount,
        String currency,
        String receipt,
        String keyId
    ) {}



    public record VerifyRequest(
        @NotBlank String razorpayPaymentId,
        @NotBlank String razorpayOrderId,
        @NotBlank String razorpaySignature
    ) {}

    // ── STEP 2: VERIFY RESPONSE ───────────────────────────────────
    public record VerifyResponse(
        boolean success,
        Long paymentId, // Internal DB ID
        String message
    ) {}

    public record RefundRequest(
        @NotNull Long paymentId,
        Long amount,
        @Pattern(regexp = "normal|optimum|instant")
        String speed
    ) {}

    // ── REFUND RESPONSE ───────────────────────────────────────────
    public record RefundResponse(
        String refundId,
        Long amountRefunded,
        String status
    ) {}

    // ── HISTORY ITEM ──────────────────────────────────────────────
    public record PaymentHistoryItem(
        Long id,
        String razorpayOrderId,
        String razorpayPaymentId,
        String productName,
        Integer amount,
        String currency,
        String status,
        String paymentMethod,       // "upi", "card", "netbanking", "wallet"
        String createdAt,
        String completedAt
    ) {}

    // ── KEY RESPONSE ──────────────────────────────────────────────
    public record KeyResponse(String keyId) {}

    // ── ERROR ─────────────────────────────────────────────────────
    public record ErrorResponse(String error, String message, int status) {}
}
