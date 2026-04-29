package com.org.dev.payment.config;

import com.razorpay.RazorpayClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class RazorpayHealthCheck implements HealthCheck {

    @ConfigProperty(name = "razorpay.key.id")
    String keyId;

    @ConfigProperty(name = "razorpay.key.secret")
    String keySecret;

    @Override
    public HealthCheckResponse call() {
        try {
            // Attempt to initialise the client — validates credentials format
            new RazorpayClient(keyId, keySecret);
            boolean isTestMode = keyId.startsWith("rzp_test_");
            return HealthCheckResponse.builder()
                .name("razorpay")
                .up()
                .withData("mode", isTestMode ? "test" : "live")
                .withData("keyId", keyId.substring(0, Math.min(keyId.length(), 12)) + "...")
                .build();
        } catch (Exception e) {
            return HealthCheckResponse.builder()
                .name("razorpay")
                .down()
                .withData("error", e.getMessage())
                .build();
        }
    }
}
