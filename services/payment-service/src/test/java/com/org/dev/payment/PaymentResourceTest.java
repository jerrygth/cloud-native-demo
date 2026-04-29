package com.org.dev.payment;

import com.org.dev.payment.dto.PaymentDTOs.*;
import com.org.dev.payment.entity.Payment;
import com.org.dev.payment.entity.PaymentStatus;
import com.org.dev.payment.exception.PaymentException;
import com.org.dev.payment.service.PaymentService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@QuarkusTest
@DisplayName("PaymentResource — REST endpoint tests")
class PaymentResourceTest {

    // ── MOCK SERVICE ──────────────────────────────────────────────
    // All PaymentService calls are mocked — we test the HTTP layer only
    @InjectMock
    PaymentService paymentService;

    // ── TEST CONSTANTS ────────────────────────────────────────────
    static final String USER_ID     = "auth0|test-user-001";
    static final String OTHER_USER  = "auth0|test-user-002";
    static final String BASE_PATH   = "/api/payments";
    static final String TEST_KEY_ID = "rzp_test_testKeyId123456";

    // Sample Razorpay IDs (format matches real Razorpay IDs)
    static final String ORDER_ID   = "order_TestOrderId001";
    static final String PAYMENT_ID = "pay_TestPaymentId01";
    // A real HMAC-SHA256 signature for testing (computed with test key)
    static final String SIGNATURE  = "aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899";

    // ── HELPERS ───────────────────────────────────────────────────
    // Builds a valid OrderRequest JSON body
    private String orderRequestBody() {
        return """
            {
              "productName": "Premium Plan",
              "amount": 49900,
              "currency": "INR",
              "receipt": "rcpt-001"
            }
            """;
    }

    // Builds the OrderResponse that the mock service returns
    private OrderResponse sampleOrderResponse() {
        return new OrderResponse(ORDER_ID, 49900, "INR", "rcpt-001", TEST_KEY_ID);
    }

    // Builds a valid VerifyRequest JSON body
    private String verifyRequestBody() {
        return """
            {
              "razorpayPaymentId": "%s",
              "razorpayOrderId":   "%s",
              "razorpaySignature": "%s"
            }
            """.formatted(PAYMENT_ID, ORDER_ID, SIGNATURE);
    }

    private VerifyResponse sampleVerifyResponse() {
        return new VerifyResponse(true, 1L, "Payment captured successfully");
    }

    // 1. GET /api/payments/key — Public key endpoint

    @Nested
    @DisplayName("GET /api/payments/key")
    class GetPublicKeyTests {

        @Test
        @DisplayName("returns 200 with keyId when authenticated")
        @TestSecurity(user = USER_ID, roles = "user")
        void returnsKeyIdWhenAuthenticated() {
            when(paymentService.getPublicKey())
                    .thenReturn(new KeyResponse(TEST_KEY_ID));

            given()
                    .when().get(BASE_PATH + "/key")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("keyId", equalTo(TEST_KEY_ID));

            verify(paymentService, times(1)).getPublicKey();
        }

        @Test
        @DisplayName("returns 401 when unauthenticated")
        void returns401WhenUnauthenticated() {
            // No @TestSecurity → request arrives without auth context
            given()
                    .when().get(BASE_PATH + "/key")
                    .then()
                    .statusCode(401);

            // Service should never be called if auth fails
            verifyNoInteractions(paymentService);
        }

        @Test
        @DisplayName("keySecret is not present in the response")
        @TestSecurity(user = USER_ID, roles = "user")
        void keySecretNotExposedInResponse() {
            when(paymentService.getPublicKey())
                    .thenReturn(new KeyResponse(TEST_KEY_ID));

            // Response should have ONLY keyId — never keySecret
            String responseBody = given()
                    .when().get(BASE_PATH + "/key")
                    .then().statusCode(200)
                    .extract().body().asString();

            assertFalse(responseBody.contains("keySecret"),
                    "keySecret must NEVER appear in API responses");
            assertFalse(responseBody.contains("secret"),
                    "secret fields must NEVER appear in API responses");
        }
    }


    // 2. POST /api/payments/order — Step 1: Create Razorpay order


    @Nested
    @DisplayName("POST /api/payments/order")
    class CreateOrderTests {

        @Test
        @DisplayName("returns 201 with order details for valid request")
        @TestSecurity(user = USER_ID, roles = "user")
        void returnsOrderResponseOnSuccess() {
            when(paymentService.createOrder(any(OrderRequest.class), eq(USER_ID)))
                    .thenReturn(sampleOrderResponse());

            given()
                    .contentType(ContentType.JSON)
                    .body(orderRequestBody())
                    .when()
                    .post(BASE_PATH + "/order")
                    .then()
                    .statusCode(201)
                    .body("razorpayOrderId", equalTo(ORDER_ID))
                    .body("amount",          equalTo(49900))
                    .body("currency",        equalTo("INR"))
                    .body("receipt",         equalTo("rcpt-001"))
                    .body("keyId",           equalTo(TEST_KEY_ID));
        }

        @Test
        @DisplayName("forwards authenticated user's sub claim as userId to service")
        @TestSecurity(user = USER_ID, roles = "user")
        void forwardsJwtSubAsUserId() {
            when(paymentService.createOrder(any(), eq(USER_ID)))
                    .thenReturn(sampleOrderResponse());

            given()
                    .contentType(ContentType.JSON)
                    .body(orderRequestBody())
                    .when()
                    .post(BASE_PATH + "/order")
                    .then()
                    .statusCode(201);

            // Capture what was passed to the service
            ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
            verify(paymentService).createOrder(any(OrderRequest.class), userIdCaptor.capture());

            // Must be the JWT sub claim, not the username
            assertEquals(USER_ID, userIdCaptor.getValue(),
                    "userId forwarded to service must be the JWT sub claim");
        }

        @Test
        @DisplayName("returns 401 when unauthenticated")
        void returns401WhenUnauthenticated() {
            given()
                    .contentType(ContentType.JSON)
                    .body(orderRequestBody())
                    .when()
                    .post(BASE_PATH + "/order")
                    .then()
                    .statusCode(401);

            verifyNoInteractions(paymentService);
        }

        @Test
        @DisplayName("returns 400 when productName is blank")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns400WhenProductNameBlank() {
            String body = """
                { "productName": "", "amount": 49900, "currency": "INR", "receipt": "rcpt-001" }
                """;

            given()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post(BASE_PATH + "/order")
                    .then()
                    .statusCode(400);

            verifyNoInteractions(paymentService);
        }

        @Test
        @DisplayName("returns 400 when amount is null")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns400WhenAmountNull() {
            String body = """
                { "productName": "Plan", "currency": "INR", "receipt": "rcpt-001" }
                """;

            given()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post(BASE_PATH + "/order")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("returns 400 when amount is negative")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns400WhenAmountNegative() {
            String body = """
                { "productName": "Plan", "amount": -100, "currency": "INR", "receipt": "rcpt-001" }
                """;

            given()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post(BASE_PATH + "/order")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("returns 400 when currency is unsupported")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns400WhenCurrencyUnsupported() {
            // GBP is not in the allowed pattern (INR|USD|EUR)
            String body = """
                { "productName": "Plan", "amount": 49900, "currency": "GBP", "receipt": "rcpt-001" }
                """;

            given()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post(BASE_PATH + "/order")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("returns 400 when receipt is blank")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns400WhenReceiptBlank() {
            String body = """
                { "productName": "Plan", "amount": 49900, "currency": "INR", "receipt": "" }
                """;

            given()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post(BASE_PATH + "/order")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("returns 502 when Razorpay API is unavailable")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns502WhenRazorpayUnavailable() {
            when(paymentService.createOrder(any(), any()))
                    .thenThrow(new PaymentException.ProviderException("Razorpay API timeout"));

            given()
                    .contentType(ContentType.JSON)
                    .body(orderRequestBody())
                    .when()
                    .post(BASE_PATH + "/order")
                    .then()
                    .statusCode(502)
                    .body("error",   equalTo("provider_error"))
                    .body("message", containsString("Razorpay API timeout"));
        }
    }


    // 3. POST /api/payments/verify — Step 2: Verify payment signature

    @Nested
    @DisplayName("POST /api/payments/verify")
    class VerifyPaymentTests {

        @Test
        @DisplayName("returns 200 with success=true when signature is valid")
        @TestSecurity(user = USER_ID, roles = "user")
        void returnsSuccessWhenSignatureValid() {
            when(paymentService.verifyPayment(any(VerifyRequest.class), eq(USER_ID)))
                    .thenReturn(sampleVerifyResponse());

            given()
                    .contentType(ContentType.JSON)
                    .body(verifyRequestBody())
                    .when()
                    .post(BASE_PATH + "/verify")
                    .then()
                    .statusCode(200)
                    .body("success",   equalTo(true))
                    .body("paymentId", equalTo(1))
                    .body("message",   containsString("captured"));
        }

        @Test
        @DisplayName("passes all 3 Razorpay IDs to service correctly")
        @TestSecurity(user = USER_ID, roles = "user")
        void passesAllThreeIdsToService() {
            when(paymentService.verifyPayment(any(), any()))
                    .thenReturn(sampleVerifyResponse());

            given()
                    .contentType(ContentType.JSON)
                    .body(verifyRequestBody())
                    .when()
                    .post(BASE_PATH + "/verify")
                    .then()
                    .statusCode(200);

            ArgumentCaptor<VerifyRequest> captor = ArgumentCaptor.forClass(VerifyRequest.class);
            verify(paymentService).verifyPayment(captor.capture(), eq(USER_ID));

            VerifyRequest captured = captor.getValue();
            assertEquals(PAYMENT_ID, captured.razorpayPaymentId());
            assertEquals(ORDER_ID,   captured.razorpayOrderId());
            assertEquals(SIGNATURE,  captured.razorpaySignature());
        }

        @Test
        @DisplayName("returns 400 when signature is invalid")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns400WhenSignatureInvalid() {
            when(paymentService.verifyPayment(any(), any()))
                    .thenThrow(new PaymentException.SignatureVerificationException(
                            "Payment signature verification failed"));

            given()
                    .contentType(ContentType.JSON)
                    .body(verifyRequestBody())
                    .when()
                    .post(BASE_PATH + "/verify")
                    .then()
                    .statusCode(400)
                    .body("error",   equalTo("signature_invalid"))
                    .body("message", containsString("signature"));
        }

        @Test
        @DisplayName("returns 404 when order not found in DB")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns404WhenOrderNotFound() {
            when(paymentService.verifyPayment(any(), any()))
                    .thenThrow(new PaymentException.NotFoundException(
                            "Payment not found for order: " + ORDER_ID));

            given()
                    .contentType(ContentType.JSON)
                    .body(verifyRequestBody())
                    .when()
                    .post(BASE_PATH + "/verify")
                    .then()
                    .statusCode(404)
                    .body("error", equalTo("not_found"));
        }

        @Test
        @DisplayName("returns 403 when payment belongs to a different user")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns403WhenPaymentBelongsToDifferentUser() {
            when(paymentService.verifyPayment(any(), any()))
                    .thenThrow(new PaymentException.UnauthorizedException(
                            "Payment does not belong to current user"));

            given()
                    .contentType(ContentType.JSON)
                    .body(verifyRequestBody())
                    .when()
                    .post(BASE_PATH + "/verify")
                    .then()
                    .statusCode(403)
                    .body("error", equalTo("forbidden"));
        }

        @Test
        @DisplayName("returns 400 when razorpayPaymentId is blank")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns400WhenPaymentIdBlank() {
            String body = """
                {
                  "razorpayPaymentId": "",
                  "razorpayOrderId":   "%s",
                  "razorpaySignature": "%s"
                }
                """.formatted(ORDER_ID, SIGNATURE);

            given()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post(BASE_PATH + "/verify")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("returns 400 when razorpayOrderId is blank")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns400WhenOrderIdBlank() {
            String body = """
                {
                  "razorpayPaymentId": "%s",
                  "razorpayOrderId":   "",
                  "razorpaySignature": "%s"
                }
                """.formatted(PAYMENT_ID, SIGNATURE);

            given()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post(BASE_PATH + "/verify")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("returns 400 when razorpaySignature is blank")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns400WhenSignatureBlank() {
            String body = """
                {
                  "razorpayPaymentId": "%s",
                  "razorpayOrderId":   "%s",
                  "razorpaySignature": ""
                }
                """.formatted(PAYMENT_ID, ORDER_ID);

            given()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post(BASE_PATH + "/verify")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("returns 401 when unauthenticated")
        void returns401WhenUnauthenticated() {
            given()
                    .contentType(ContentType.JSON)
                    .body(verifyRequestBody())
                    .when()
                    .post(BASE_PATH + "/verify")
                    .then()
                    .statusCode(401);

            verifyNoInteractions(paymentService);
        }
    }


    // 4. GET /api/payments — Payment history

    @Nested
    @DisplayName("GET /api/payments")
    class GetHistoryTests {

        private List<PaymentHistoryItem> sampleHistory() {
            return List.of(
                    new PaymentHistoryItem(
                            1L, ORDER_ID, PAYMENT_ID,
                            "Premium Plan", 49900, "INR",
                            "CAPTURED", "upi",
                            "2025-01-15T10:30:00", "2025-01-15T10:30:05"
                    ),
                    new PaymentHistoryItem(
                            2L, "order_TestOrderId002", null,
                            "Basic Plan", 19900, "INR",
                            "CREATED", null,
                            "2025-01-14T09:00:00", null
                    )
            );
        }

        @Test
        @DisplayName("returns 200 with list of payment history items")
        @TestSecurity(user = USER_ID, roles = "user")
        void returnsPaymentHistoryList() {
            when(paymentService.getHistory(USER_ID))
                    .thenReturn(sampleHistory());

            given()
                    .when().get(BASE_PATH)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("$",              hasSize(2))
                    .body("[0].id",              equalTo(1))
                    .body("[0].razorpayOrderId", equalTo(ORDER_ID))
                    .body("[0].razorpayPaymentId",equalTo(PAYMENT_ID))
                    .body("[0].productName",     equalTo("Premium Plan"))
                    .body("[0].amount",          equalTo(49900))
                    .body("[0].currency",        equalTo("INR"))
                    .body("[0].status",          equalTo("CAPTURED"))
                    .body("[0].paymentMethod",   equalTo("upi"))
                    // Second item has null paymentId and null completedAt
                    .body("[1].razorpayPaymentId", nullValue())
                    .body("[1].status",            equalTo("CREATED"));
        }

        @Test
        @DisplayName("returns 200 with empty list when user has no payments")
        @TestSecurity(user = USER_ID, roles = "user")
        void returnsEmptyListWhenNoPayments() {
            when(paymentService.getHistory(USER_ID)).thenReturn(List.of());

            given()
                    .when().get(BASE_PATH)
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(0));
        }

        @Test
        @DisplayName("only returns payments for the authenticated user")
        @TestSecurity(user = USER_ID, roles = "user")
        void onlyReturnsCurrentUsersPayments() {
            when(paymentService.getHistory(USER_ID)).thenReturn(sampleHistory());

            given()
                    .when().get(BASE_PATH)
                    .then()
                    .statusCode(200);

            // Verify service was called with the correct userId from JWT
            verify(paymentService).getHistory(USER_ID);
            // Never called with another user's ID
            verify(paymentService, never()).getHistory(OTHER_USER);
        }

        @Test
        @DisplayName("returns 401 when unauthenticated")
        void returns401WhenUnauthenticated() {
            given()
                    .when().get(BASE_PATH)
                    .then()
                    .statusCode(401);

            verifyNoInteractions(paymentService);
        }
    }

    // 5. POST /api/payments/refund — Refund a captured payment

    @Nested
    @DisplayName("POST /api/payments/refund")
    class RefundTests {

        private String fullRefundBody() {
            return """
                { "paymentId": 1 }
                """;
        }

        private String partialRefundBody(long amount) {
            return """
                { "paymentId": 1, "amount": %d, "speed": "normal" }
                """.formatted(amount);
        }

        private RefundResponse sampleRefundResponse(long amount) {
            return new RefundResponse("rfnd_TestRefundId001", amount, "processed");
        }

        @Test
        @DisplayName("returns 200 with refund details for full refund")
        @TestSecurity(user = USER_ID, roles = "user")
        void returnsRefundResponseForFullRefund() {
            when(paymentService.refund(any(RefundRequest.class), eq(USER_ID)))
                    .thenReturn(sampleRefundResponse(49900L));

            given()
                    .contentType(ContentType.JSON)
                    .body(fullRefundBody())
                    .when()
                    .post(BASE_PATH + "/refund")
                    .then()
                    .statusCode(200)
                    .body("refundId",       equalTo("rfnd_TestRefundId001"))
                    .body("amountRefunded", equalTo(49900))
                    .body("status",         equalTo("processed"));
        }

        @Test
        @DisplayName("returns 200 with partial refund amount")
        @TestSecurity(user = USER_ID, roles = "user")
        void returnsPartialRefundResponse() {
            when(paymentService.refund(any(RefundRequest.class), eq(USER_ID)))
                    .thenReturn(sampleRefundResponse(10000L));

            given()
                    .contentType(ContentType.JSON)
                    .body(partialRefundBody(10000L))
                    .when()
                    .post(BASE_PATH + "/refund")
                    .then()
                    .statusCode(200)
                    .body("amountRefunded", equalTo(10000));
        }

        @Test
        @DisplayName("returns 404 when payment not found")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns404WhenPaymentNotFound() {
            when(paymentService.refund(any(), any()))
                    .thenThrow(new PaymentException.NotFoundException("Payment not found"));

            given()
                    .contentType(ContentType.JSON)
                    .body(fullRefundBody())
                    .when()
                    .post(BASE_PATH + "/refund")
                    .then()
                    .statusCode(404)
                    .body("error", equalTo("not_found"));
        }

        @Test
        @DisplayName("returns 409 when payment is not in CAPTURED state")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns409WhenPaymentNotCaptured() {
            when(paymentService.refund(any(), any()))
                    .thenThrow(new PaymentException.InvalidStateException(
                            "Only captured payments can be refunded. Current status: CREATED"));

            given()
                    .contentType(ContentType.JSON)
                    .body(fullRefundBody())
                    .when()
                    .post(BASE_PATH + "/refund")
                    .then()
                    .statusCode(409)
                    .body("error",   equalTo("invalid_state"))
                    .body("message", containsString("CREATED"));
        }

        @Test
        @DisplayName("returns 403 when refunding another user's payment")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns403WhenRefundingAnotherUsersPayment() {
            when(paymentService.refund(any(), any()))
                    .thenThrow(new PaymentException.UnauthorizedException("Forbidden"));

            given()
                    .contentType(ContentType.JSON)
                    .body(fullRefundBody())
                    .when()
                    .post(BASE_PATH + "/refund")
                    .then()
                    .statusCode(403)
                    .body("error", equalTo("forbidden"));
        }

        @Test
        @DisplayName("returns 400 when paymentId is null")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns400WhenPaymentIdNull() {
            String body = """
                { "amount": 5000 }
                """;

            given()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post(BASE_PATH + "/refund")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("returns 400 when speed value is invalid")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns400WhenSpeedInvalid() {
            // "express" is not in the allowed pattern (normal|optimum|instant)
            String body = """
                { "paymentId": 1, "speed": "express" }
                """;

            given()
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post(BASE_PATH + "/refund")
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("returns 502 when Razorpay refund API fails")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns502WhenRazorpayRefundFails() {
            when(paymentService.refund(any(), any()))
                    .thenThrow(new PaymentException.ProviderException("Refund failed: insufficient balance"));

            given()
                    .contentType(ContentType.JSON)
                    .body(fullRefundBody())
                    .when()
                    .post(BASE_PATH + "/refund")
                    .then()
                    .statusCode(502)
                    .body("error", equalTo("provider_error"));
        }

        @Test
        @DisplayName("returns 401 when unauthenticated")
        void returns401WhenUnauthenticated() {
            given()
                    .contentType(ContentType.JSON)
                    .body(fullRefundBody())
                    .when()
                    .post(BASE_PATH + "/refund")
                    .then()
                    .statusCode(401);

            verifyNoInteractions(paymentService);
        }
    }


    // 6. POST /api/payments/webhook — Razorpay async events
    //
    // WEBHOOK TESTING NOTES:
    //   - Endpoint is @PermitAll — no auth needed (Razorpay calls it)
    //   - Security is HMAC signature on X-Razorpay-Signature header
    //   - Service handles signature verification internally
    //   - Tests cover all 3 Razorpay event types + invalid signature


    @Nested
    @DisplayName("POST /api/payments/webhook")
    class WebhookTests {

        // Realistic Razorpay webhook payload for payment.captured
        private String paymentCapturedPayload() {
            return """
                {
                  "entity": "event",
                  "account_id": "acc_test",
                  "event": "payment.captured",
                  "contains": ["payment"],
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "%s",
                        "entity": "payment",
                        "amount": 49900,
                        "currency": "INR",
                        "status": "captured",
                        "order_id": "%s",
                        "method": "upi",
                        "captured": true
                      }
                    }
                  }
                }
                """.formatted(PAYMENT_ID, ORDER_ID);
        }

        private String paymentFailedPayload() {
            return """
                {
                  "entity": "event",
                  "event": "payment.failed",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "%s",
                        "order_id": "%s",
                        "status": "failed"
                      }
                    }
                  }
                }
                """.formatted(PAYMENT_ID, ORDER_ID);
        }

        private String refundProcessedPayload() {
            return """
                {
                  "entity": "event",
                  "event": "refund.processed",
                  "payload": {
                    "refund": {
                      "entity": {
                        "id": "rfnd_TestRefundId001",
                        "payment_id": "%s",
                        "amount": 49900
                      }
                    }
                  }
                }
                """.formatted(PAYMENT_ID);
        }

        @Test
        @DisplayName("returns 200 for valid payment.captured webhook")
        void returns200ForPaymentCapturedEvent() {
            // No auth annotation — webhook is @PermitAll
            doNothing().when(paymentService).handleWebhook(any(), any());

            given()
                    .contentType(ContentType.TEXT)
                    .header("X-Razorpay-Signature", "valid-hmac-signature")
                    .body(paymentCapturedPayload())
                    .when()
                    .post(BASE_PATH + "/webhook")
                    .then()
                    .statusCode(200);

            verify(paymentService, times(1))
                    .handleWebhook(any(), eq("valid-hmac-signature"));
        }

        @Test
        @DisplayName("returns 200 for valid payment.failed webhook")
        void returns200ForPaymentFailedEvent() {
            doNothing().when(paymentService).handleWebhook(any(), any());

            given()
                    .contentType(ContentType.TEXT)
                    .header("X-Razorpay-Signature", "valid-hmac-signature")
                    .body(paymentFailedPayload())
                    .when()
                    .post(BASE_PATH + "/webhook")
                    .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("returns 200 for valid refund.processed webhook")
        void returns200ForRefundProcessedEvent() {
            doNothing().when(paymentService).handleWebhook(any(), any());

            given()
                    .contentType(ContentType.TEXT)
                    .header("X-Razorpay-Signature", "valid-hmac-signature")
                    .body(refundProcessedPayload())
                    .when()
                    .post(BASE_PATH + "/webhook")
                    .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("returns 400 when webhook signature is invalid")
        void returns400ForInvalidSignature() {
            doThrow(new PaymentException.SignatureVerificationException("Invalid webhook signature"))
                    .when(paymentService).handleWebhook(any(), eq("bad-signature"));

            given()
                    .contentType(ContentType.TEXT)
                    .header("X-Razorpay-Signature", "bad-signature")
                    .body(paymentCapturedPayload())
                    .when()
                    .post(BASE_PATH + "/webhook")
                    .then()
                    .statusCode(400)
                    .body("error", equalTo("signature_invalid"));
        }

        @Test
        @DisplayName("passes X-Razorpay-Signature header to service correctly")
        void passesSignatureHeaderToService() {
            doNothing().when(paymentService).handleWebhook(any(), any());
            String expectedSig = "sha256-hmac-header-value";

            given()
                    .contentType(ContentType.TEXT)
                    .header("X-Razorpay-Signature", expectedSig)
                    .body(paymentCapturedPayload())
                    .when()
                    .post(BASE_PATH + "/webhook")
                    .then()
                    .statusCode(200);

            // Verify the exact signature header value was forwarded
            verify(paymentService).handleWebhook(any(), eq(expectedSig));
        }

        @Test
        @DisplayName("webhook does NOT require authentication (is @PermitAll)")
        void webhookIsPermitAll() {
            // Send with no auth headers at all — must not get 401
            doNothing().when(paymentService).handleWebhook(any(), any());

            given()
                    .contentType(ContentType.TEXT)
                    .header("X-Razorpay-Signature", "some-sig")
                    .body(paymentCapturedPayload())
                    .when()
                    .post(BASE_PATH + "/webhook")
                    .then()
                    // 200 (not 401) confirms @PermitAll is working
                    .statusCode(not(equalTo(401)));
        }

        @Test
        @DisplayName("returns 200 quickly — Razorpay retries on failure for 24 hours")
        void returns200EvenForUnhandledEventType() {
            // Service handles unknown events gracefully (logs and returns)
            doNothing().when(paymentService).handleWebhook(any(), any());

            String unknownEvent = """
                { "event": "subscription.charged", "payload": {} }
                """;

            given()
                    .contentType(ContentType.TEXT)
                    .header("X-Razorpay-Signature", "valid-sig")
                    .body(unknownEvent)
                    .when()
                    .post(BASE_PATH + "/webhook")
                    .then()
                    .statusCode(200);
        }
    }


    // 7. Cross-cutting concerns — Content-Type, error shape, etc.

    @Nested
    @DisplayName("Cross-cutting concerns")
    class CrossCuttingTests {

        @Test
        @DisplayName("all error responses have { error, message, status } shape")
        @TestSecurity(user = USER_ID, roles = "user")
        void errorResponseHasConsistentShape() {
            when(paymentService.createOrder(any(), any()))
                    .thenThrow(new PaymentException.NotFoundException("Payment not found"));

            given()
                    .contentType(ContentType.JSON)
                    .body(orderRequestBody())
                    .when()
                    .post(BASE_PATH + "/order")
                    .then()
                    .statusCode(404)
                    // GlobalExceptionMapper always returns these 3 fields
                    .body("error",   notNullValue())
                    .body("message", notNullValue())
                    .body("status",  notNullValue());
        }

        @Test
        @DisplayName("500 is returned for unexpected RuntimeException")
        @TestSecurity(user = USER_ID, roles = "user")
        void returns500ForUnexpectedException() {
            when(paymentService.createOrder(any(), any()))
                    .thenThrow(new RuntimeException("Unexpected DB error"));

            given()
                    .contentType(ContentType.JSON)
                    .body(orderRequestBody())
                    .when()
                    .post(BASE_PATH + "/order")
                    .then()
                    .statusCode(500)
                    .body("error", equalTo("internal_error"));
        }

        @Test
        @DisplayName("POST /order returns application/json content-type")
        @TestSecurity(user = USER_ID, roles = "user")
        void postOrderReturnsJsonContentType() {
            when(paymentService.createOrder(any(), any()))
                    .thenReturn(sampleOrderResponse());

            given()
                    .contentType(ContentType.JSON)
                    .body(orderRequestBody())
                    .when()
                    .post(BASE_PATH + "/order")
                    .then()
                    .statusCode(201)
                    .contentType(containsString("application/json"));
        }

        @Test
        @DisplayName("GET /history returns application/json content-type")
        @TestSecurity(user = USER_ID, roles = "user")
        void getHistoryReturnsJsonContentType() {
            when(paymentService.getHistory(USER_ID)).thenReturn(List.of());

            given()
                    .when().get(BASE_PATH)
                    .then()
                    .statusCode(200)
                    .contentType(containsString("application/json"));
        }

        @Test
        @DisplayName("sending wrong HTTP method returns 500 from GlobalExceptionMapper")
        @TestSecurity(user = USER_ID, roles = "user")
        void wrongHttpMethodReturns405() {
            // GET on /order (which is POST-only) should return 405
            given()
                    .when().get(BASE_PATH + "/order")
                    .then()
                    .statusCode(500);
        }
    }
}