package com.org.dev.payment.entity;

// PAYMENT STATUS — Razorpay lifecycle
//
// Razorpay payment lifecycle:
//
//   CREATED   → Order created via Razorpay API (orderId assigned)
//               Awaiting user to open popup and pay
//
//   AUTHORIZED → Payment received but not yet captured
//               (only in manual capture mode — most use auto-capture)
//
//   CAPTURED  → Payment verified + captured → money deducted from user
//               This is the "successful payment" state
//
//   FAILED    → User closed popup, payment declined, or verification failed
//
//   REFUNDED  → Full refund processed
//
//   PARTIALLY_REFUNDED → Partial refund processed
//
// Razorpay vs Stripe status mapping:
//   Stripe COMPLETED    → Razorpay CAPTURED
//   Stripe PENDING      → Razorpay CREATED / AUTHORIZED
//   Stripe CANCELLED    → Razorpay FAILED (user closed popup)

public enum PaymentStatus {
    CREATED,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED
}