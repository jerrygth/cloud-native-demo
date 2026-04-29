package com.org.dev.payment.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;


//@RegisterRestClient(configKey = "order-service")
@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface OrderServiceClient {

    /**
     * Notify the Order Service that payment was confirmed     *
     */
    @POST
    @Path("/{orderId}/confirm-payment")
    void confirmPayment(@PathParam("orderId") String orderId);

    /**
     * Get order details to validate the checkout request
     */
    @GET
    @Path("/{orderId}")
    OrderDetails getOrder(@PathParam("orderId") String orderId);

    /**
     * Simple DTO for order details response
     */
    record OrderDetails(
        String orderId,
        String userId,
        Long totalAmount,
        String status
    ) {}
}
