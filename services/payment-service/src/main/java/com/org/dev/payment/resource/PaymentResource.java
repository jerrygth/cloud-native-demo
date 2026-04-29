package com.org.dev.payment.resource;

import com.org.dev.payment.dto.PaymentDTOs.*;
import com.org.dev.payment.service.PaymentService;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import jakarta.annotation.security.PermitAll;
import org.eclipse.microprofile.rest.client.inject.RestClient;


import java.util.List;


@Path("/api/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResource {

    @Inject
    PaymentService paymentService;

    @Inject
    SecurityIdentity identity;

    private String userId() {
        return identity.getPrincipal().getName();
    }


    // ── GET PUBLIC KEY ─────────────────────────────────────────────
    // Returns the Razorpay key_id

    @GET
    @Path("/key")
    @Authenticated
    public KeyResponse getPublicKey() {
        return paymentService.getPublicKey();
    }

    // Returns orderId + amount for the Razorpay popup.
    @POST
    @Path("/order")
    @Authenticated
    public Response createOrder(@Valid OrderRequest request) {
        OrderResponse response = paymentService.createOrder(request, userId());
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    // Verifies HMAC signature → captures payment → updates DB.
    @POST
    @Path("/verify")
    @Authenticated
    public Response verifyPayment(@Valid VerifyRequest request) {
        VerifyResponse response = paymentService.verifyPayment(request, userId());
        return Response.ok(response).build();
    }

    @GET
    @Authenticated
    public List<PaymentHistoryItem> getHistory() {
        return paymentService.getHistory(userId());
    }

    @POST
    @Path("/refund")
    @Authenticated
    public Response refund(@Valid RefundRequest request) {
        RefundResponse response = paymentService.refund(request, userId());
        return Response.ok(response).build();
    }

    // ── WEBHOOK ────────────────────────────────────────────────────
    @POST
    @Path("/webhook")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @PermitAll
    public Response handleWebhook(
            String payload,
            @HeaderParam("X-Razorpay-Signature") String razorpaySignature) {
        System.out.println("Inside webhook handler. Payload: ");
        paymentService.handleWebhook(payload, razorpaySignature);
        return Response.ok().build();
    }
}