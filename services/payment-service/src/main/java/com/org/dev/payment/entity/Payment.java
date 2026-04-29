package com.org.dev.payment.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "payments")
public class Payment extends PanacheEntity {

    @Column(name = "user_id", nullable = false)
    public String userId;

    @Column(name = "product_name", nullable = false)
    public String productName;

    @Column(name = "razorpay_order_id", unique = true)
    public String razorpayOrderId;

    @Column(name = "razorpay_payment_id", unique = true)
    public String razorpayPaymentId;

    @Column(name = "amount", nullable = false)
    public Integer amount;

    @Column(name = "currency", nullable = false)
    public String currency = "INR";

    // ── STATUS ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public PaymentStatus status = PaymentStatus.CREATED;

    @Column(name = "payment_method")
    public String paymentMethod;

    @Column(name = "receipt")
    public String receipt;

    // ── TIMESTAMPS ────────────────────────────────────────────────
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "captured_at")
    public LocalDateTime capturedAt;

    // ── QUERIES ───────────────────────────────────────────────────
    public static List<Payment> findByUserId(String userId) {
        return list("userId = ?1 ORDER BY createdAt DESC", userId);
    }

    public static Payment findByRazorpayOrderId(String razorpayOrderId) {
        return find("razorpayOrderId", razorpayOrderId).firstResult();
    }

    public static Payment findByRazorpayPaymentId(String razorpayPaymentId) {
        return find("razorpayPaymentId", razorpayPaymentId).firstResult();
    }
}