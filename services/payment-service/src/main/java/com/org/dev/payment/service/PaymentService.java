package com.org.dev.payment.service;

import com.org.dev.payment.client.OrderServiceClient;
import com.org.dev.payment.dto.PaymentDTOs.*;
import com.org.dev.payment.entity.Payment;
import com.org.dev.payment.entity.PaymentStatus;
import com.org.dev.payment.exception.PaymentException;
import com.razorpay.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.json.JSONObject;
import org.jboss.logging.Logger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PaymentService {

    private static final Logger log = Logger.getLogger(PaymentService.class);
    @ConfigProperty(name = "razorpay.key.id")
    String keyId;

    @ConfigProperty(name = "razorpay.key.secret")
    String keySecret;

    @ConfigProperty(name = "razorpay.webhook.secret")
    String webhookSecret;



    // ── RAZORPAY CLIENT ───────────────────────────────────────────
    // Lazily initialised to allow config injection first
    private RazorpayClient razorpay() {
        try {
            return new RazorpayClient(keyId, keySecret);
        } catch (RazorpayException e) {
            throw new PaymentException.ConfigurationException(
                    "Failed to initialise Razorpay client: " + e.getMessage());
        }
    }

     // STEP 1 — CREATE RAZORPAY ORDER
     @Transactional
    public OrderResponse createOrder(OrderRequest request, String userId) {
        try {
            // ── BUILD RAZORPAY ORDER REQUEST ──────────────────────
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.amount());     // in paise
            orderRequest.put("currency", request.currency() != null ? request.currency() : "INR");
            orderRequest.put("receipt", request.receipt());
            orderRequest.put("payment_capture", 1);           // auto-capture on payment
            log.debug("Before calling Razorpay API with orderRequest: " + orderRequest.toString());
            // ── CALL RAZORPAY API ──────────────────────────────────
            Order razorpayOrder = razorpay().orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            // ── SAVE TO DB ─────────────────────────────────────────
            Payment payment = new Payment();
            payment.userId = userId;
            payment.productName = request.productName();
            payment.amount = request.amount();
            payment.currency = razorpayOrder.get("currency");
            payment.receipt = request.receipt();
            payment.razorpayOrderId = razorpayOrderId;
            payment.status = PaymentStatus.CREATED;
            payment.persist();

            // keyId is returned so React can initialise the Razorpay popup
            return new OrderResponse(
                    razorpayOrderId,
                    (int) razorpayOrder.get("amount"),
                    razorpayOrder.get("currency"),
                    razorpayOrder.get("receipt"),
                    keyId   // Public key — safe to expose
            );

        } catch (RazorpayException e) {
            throw new PaymentException.ProviderException("Razorpay order creation failed: " + e.getMessage());
        }
    }

    // STEP 2 — VERIFY PAYMENT SIGNATURE
    // The signature = HMAC-SHA256(orderId + "|" + paymentId, keySecret)
    @Transactional
    public VerifyResponse verifyPayment(VerifyRequest request, String userId) {
        // ── VERIFY SIGNATURE ──────────────────────────────────────
        JSONObject signatureParams = new JSONObject();
        signatureParams.put("razorpay_order_id", request.razorpayOrderId());
        signatureParams.put("razorpay_payment_id", request.razorpayPaymentId());
        signatureParams.put("razorpay_signature", request.razorpaySignature());

        try {
            boolean isValid = Utils.verifyPaymentSignature(signatureParams, keySecret);
            if (!isValid) {
                throw new PaymentException.SignatureVerificationException(
                        "Payment signature verification failed");
            }
        } catch (RazorpayException e) {
            throw new PaymentException.SignatureVerificationException(
                    "Signature verification error: " + e.getMessage());
        }

        // ── LOAD PAYMENT FROM DB ──────────────────────────────────
        Payment payment = Payment.findByRazorpayOrderId(request.razorpayOrderId());
        if (payment == null) {
            throw new PaymentException.NotFoundException(
                    "Payment not found for order: " + request.razorpayOrderId());
        }
        if (!payment.userId.equals(userId)) {
            throw new PaymentException.UnauthorizedException(
                    "Payment does not belong to current user");
        }

        // ── FETCH PAYMENT DETAILS FROM RAZORPAY ───────────────────
        // Get the payment method (upi/card/netbanking) and confirm status
        try {
            com.razorpay.Payment razorpayPayment =
                    razorpay().payments.fetch(request.razorpayPaymentId());

            payment.razorpayPaymentId = request.razorpayPaymentId();
            payment.paymentMethod = razorpayPayment.get("method"); // "upi", "card", etc.
            payment.status = PaymentStatus.CAPTURED;
            payment.capturedAt = LocalDateTime.now();
            payment.persist();



        } catch (RazorpayException e) {
            // Signature already verified — payment is real
            // Just update with what we know even if fetch fails
            payment.razorpayPaymentId = request.razorpayPaymentId();
            payment.status = PaymentStatus.CAPTURED;
            payment.capturedAt = LocalDateTime.now();
            payment.persist();
        }

        return new VerifyResponse(true, payment.id,
                "Payment captured successfully");
    }

    // PAYMENT HISTORY
    public List<PaymentHistoryItem> getHistory(String userId) {
        return Payment.findByUserId(userId).stream()
                .map(p -> new PaymentHistoryItem(
                        p.id,
                        p.razorpayOrderId,
                        p.razorpayPaymentId,
                        p.productName,
                        p.amount,
                        p.currency,
                        p.status.name(),
                        p.paymentMethod,
                        p.createdAt != null ? p.createdAt.toString() : null,
                        p.capturedAt != null ? p.capturedAt.toString() : null
                ))
                .toList();
    }


    // REFUND
    @Transactional
    public RefundResponse refund(com.org.dev.payment.dto.PaymentDTOs.RefundRequest request, String userId) {
        Payment payment = Payment.<Payment>findById(request.paymentId());
        if (payment == null) throw new PaymentException.NotFoundException("Payment not found");
        if (!payment.userId.equals(userId)) throw new PaymentException.UnauthorizedException("Forbidden");
        if (payment.status != PaymentStatus.CAPTURED) {
            throw new PaymentException.InvalidStateException(
                    "Only captured payments can be refunded. Current status: " + payment.status);
        }

        try {
            JSONObject refundRequest = new JSONObject();
            // If no amount specified → full refund
            if (request.amount() != null && request.amount() > 0) {
                refundRequest.put("amount", request.amount());
            }
            refundRequest.put("speed", request.speed() != null ? request.speed() : "normal");
            refundRequest.put("notes", new JSONObject(Map.of(
                    "reason", "Customer requested refund"
            )));

            Refund refund = razorpay().payments.refund(payment.razorpayPaymentId, refundRequest);

            // Determine if full or partial refund
            long refundedAmount = ((Number) refund.get("amount")).longValue();
            payment.status = refundedAmount >= payment.amount
                    ? PaymentStatus.REFUNDED
                    : PaymentStatus.PARTIALLY_REFUNDED;
            payment.persist();

            return new RefundResponse(
                    refund.get("id"),
                    refundedAmount,
                    refund.get("status")
            );

        } catch (RazorpayException e) {
            throw new PaymentException.ProviderException("Refund failed: " + e.getMessage());
        }
    }


    // WEBHOOK HANDLER
    // Razorpay sends webhooks for async events:
    //   payment.captured — payment confirmed (use for order fulfillment)
    //   payment.failed   — user failed to pay
    //   refund.processed — refund completed
    //
    // IMPORTANT: Webhook signature is verified using webhookSecret
    //            (different from keySecret used for payment signature)
    @Transactional
    public void handleWebhook(String payload, String razorpaySignatureHeader) {
        // ── VERIFY WEBHOOK SIGNATURE ──────────────────────────────
        try {
            boolean isValid = Utils.verifyWebhookSignature(
                    payload, razorpaySignatureHeader, webhookSecret);
            if (!isValid) {
                throw new PaymentException.SignatureVerificationException(
                        "Invalid webhook signature");
            }
        } catch (RazorpayException e) {
            throw new PaymentException.SignatureVerificationException(
                    "Webhook signature error: " + e.getMessage());
        }

        // ── PARSE EVENT ───────────────────────────────────────────
        JSONObject event = new JSONObject(payload);
        String eventType = event.getString("event");

        switch (eventType) {
            case "payment.captured" -> {
                JSONObject paymentEntity = event
                        .getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String orderId = paymentEntity.getString("order_id");
                String paymentId = paymentEntity.getString("id");
                String method = paymentEntity.optString("method", "unknown");

                Payment payment = Payment.findByRazorpayOrderId(orderId);
                if (payment != null && payment.status != PaymentStatus.CAPTURED) {
                    payment.razorpayPaymentId = paymentId;
                    payment.paymentMethod = method;
                    payment.status = PaymentStatus.CAPTURED;
                    payment.capturedAt = LocalDateTime.now();
                    payment.persist();
                }
            }

            case "payment.failed" -> {
                JSONObject paymentEntity = event
                        .getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");
                String orderId = paymentEntity.getString("order_id");

                Payment payment = Payment.findByRazorpayOrderId(orderId);
                if (payment != null) {
                    payment.status = PaymentStatus.FAILED;
                    payment.persist();
                }
            }

            case "refund.processed" -> {
                // Webhook just confirms it completed async
            }

            default -> {
                // Log unhandled events — don't fail (Razorpay will retry on non-200)
                System.out.println("Unhandled webhook event: " + eventType);
            }
        }
    }

    // ── GET PUBLIC KEY ─────────────────────────────────────────────
    // Safe endpoint — returns only the public key_id for frontend use
    public KeyResponse getPublicKey() {
        return new KeyResponse(keyId);
    }
}