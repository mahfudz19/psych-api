package com.psycorp.psychapi.feature.subscription.api;

import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.psycorp.psychapi.feature.subscription.api.dto.request.CheckoutRequest;
import com.psycorp.psychapi.feature.subscription.api.dto.response.CheckoutResponse;
import com.psycorp.psychapi.feature.subscription.api.dto.response.TransactionDetailResponse;
import com.psycorp.psychapi.feature.subscription.model.Subscription;
import com.psycorp.psychapi.feature.subscription.service.TransactionService;
import com.psycorp.psychapi.feature.user.model.User;
import com.psycorp.psychapi.shared.response.ResponseHelper;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/transactions")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Transactions", description = "API untuk transaksi dan checkout pembayaran")
@SecurityScheme(securitySchemeName = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT Bearer token authentication")
public class TransactionResource {

    @Inject
    TransactionService transactionService;

    @POST
    @Path("/checkout")
    @Operation(summary = "Checkout pembelian paket langganan")
    public Response checkout(@Valid CheckoutRequest request, @Context ContainerRequestContext requestContext) {
        User user = (User) requestContext.getProperty("validatedUser");
        if (user == null) throw new ForbiddenException("Authentication required");

        Subscription.SubscriberType subscriberType;
        ObjectId buyerId;

        if (user.getAccountType() == User.AccountType.ORGANIZATION) {
            if (user.getOrganizationRole() != User.OrganizationRole.owner && 
                user.getOrganizationRole() != User.OrganizationRole.admin) {
                throw new ForbiddenException("Hanya Owner atau Admin yang dapat melakukan pembelian untuk perusahaan.");
            }
            subscriberType = Subscription.SubscriberType.ORGANIZATION;
            buyerId = user.getOrganizationId();
        } else {
            subscriberType = Subscription.SubscriberType.USER;
            buyerId = user.getId();
        }

        // Panggil Service
        TransactionService.TransactionResult result = transactionService.checkout(
            buyerId, user.getEmail(), request.planCode(), subscriberType
        );

        // Susun Response dengan data Bank/QRIS dari Xendit
        CheckoutResponse responseData = new CheckoutResponse(
            result.transaction().getReferenceId(),
            result.transaction().getCheckoutUrl(),
            result.transaction().getStatus(),
            result.invoiceData().get("available_banks"),
            result.invoiceData().get("available_qr_codes"),
            result.invoiceData().get("available_ewallets")
        );

        return ResponseHelper.created(responseData, "Checkout berhasil");
    }

    @GET
    @Path("/{referenceId}")
    @Operation(summary = "Cek detail dan status transaksi")
    public Response getTransaction(@PathParam("referenceId") String referenceId, @Context ContainerRequestContext requestContext) {
        User user = (User) requestContext.getProperty("validatedUser");
        if (user == null) throw new ForbiddenException("Authentication required");

        ObjectId buyerId = user.getAccountType() == User.AccountType.ORGANIZATION ? user.getOrganizationId() : user.getId();

        TransactionService.TransactionResult result = transactionService.getTransactionDetail(referenceId, buyerId);

        TransactionDetailResponse responseData = new TransactionDetailResponse(
            result.transaction().getReferenceId(),
            result.transaction().getCheckoutUrl(),
            result.transaction().getStatus(),
            result.invoiceData().get("available_banks"),
            result.invoiceData().get("available_qr_codes"),
            result.invoiceData().get("available_ewallets")
        );

        return ResponseHelper.ok(responseData, "Detail transaksi berhasil diambil");
    }
}