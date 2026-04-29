package com.org.dev.payment.repository;

import com.org.dev.payment.entity.Payment;
import com.org.dev.payment.entity.PaymentStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PaymentRepository implements PanacheRepository<Payment> {

    public List<Payment> findByUserId(String userId, int page, int pageSize) {
        return find("userId = ?1 ORDER BY createdAt DESC", userId)
                .page(page, pageSize)    // built-in pagination — no Pageable needed!
                .list();
    }

    public long countByUserId(String userId) {
        return count("userId", userId);
    }

    public Payment findByStripeSessionId(String sessionId) {
        return find("stripeSessionId", sessionId).firstResult();
    }

    public Payment findByIdempotencyKey(String key) {
        return find("idempotencyKey", key).firstResult();
    }

    /**
     * Named parameters (alternative to positional params)
     * More readable for complex queries
     */
    public List<Payment> findByUserIdAndStatus(String userId, PaymentStatus status) {
        return list("userId = :userId AND status = :status",
                Map.of("userId", userId, "status", status));
    }

    /**
     * Native SQL query when HQL isn't enough
     * Panache supports native queries too
     */
    public List<Payment> findRecentPaymentsNative(String userId, int limit) {
        return getEntityManager()
                .createNativeQuery(
                    "SELECT * FROM payments WHERE user_id = ?1 " +
                    "ORDER BY created_at DESC LIMIT ?2",
                    Payment.class
                )
                .setParameter(1, userId)
                .setParameter(2, limit)
                .getResultList();
    }
}
