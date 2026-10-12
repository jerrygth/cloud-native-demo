package com.org.dev.payment.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


@ApplicationScoped
@RequiredArgsConstructor
public class PaymentMetrics {

    private final MeterRegistry registry;

    private final Timer verificationSuccessTimer;
    private final Timer verificationFailureTimer;

    private final Map<String, Counter> orderCreatedCounters = new ConcurrentHashMap<>();

    public PaymentMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.verificationSuccessTimer = Timer.builder("payment.verification.duration")
                .tag("outcome", "success")
                .publishPercentileHistogram()
                .register(registry);

        this.verificationFailureTimer = Timer.builder("payment.verification.duration")
                .tag("outcome", "failure")
                .publishPercentileHistogram()
                .register(registry);
    }

    public void recordVerification(long durationNanos, boolean success) {
        Timer timer = success
                ? verificationSuccessTimer
                : verificationFailureTimer;

        timer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void incrementOrderCreated(String currency) {
        Counter counter = orderCreatedCounters.computeIfAbsent(
                currency,
                c -> Counter.builder("payment.orders.created")
                        .tag("currency", c)
                        .register(registry)
        );

        counter.increment();
    }
}