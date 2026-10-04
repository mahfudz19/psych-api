package com.psycorp.psychapi.feature.subscription.api;

import java.util.List;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.mongodb.client.model.Filters;
import com.psycorp.psychapi.feature.subscription.api.dto.request.CheckoutRequest;
import com.psycorp.psychapi.feature.subscription.api.dto.request.TransactionHistoryRequest;
import com.psycorp.psychapi.feature.subscription.api.dto.response.AdminTransactionResponse;
import com.psycorp.psychapi.feature.subscription.api.dto.response.CheckoutResponse;
import com.psycorp.psychapi.feature.subscription.api.dto.response.TransactionDetailResponse;
import com.psycorp.psychapi.feature.subscription.api.dto.response.TransactionHistoryResponse;
import com.psycorp.psychapi.feature.subscription.api.dto.response.TransactionStatusResponse;
import com.psycorp.psychapi.feature.subscription.models.Subscription;
import com.psycorp.psychapi.feature.subscription.models.Transaction;
import com.psycorp.psychapi.feature.subscription.service.TransactionService;
import com.psycorp.psychapi.feature.user.models.User;
import com.psycorp.psychapi.shared.response.PaginationMeta;
import com.psycorp.psychapi.shared.response.ResponseHelper;
import com.psycorp.psychapi.shared.util.MongoFilter;

import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
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

    @GET
    @Operation(summary = "Get transaction history", description = "Melihat riwayat transaksi user/organisasi saat ini dengan paginasi")
    public Response getHistory(@BeanParam TransactionHistoryRequest request, @Context ContainerRequestContext requestContext) {
        User user = (User) requestContext.getProperty("validatedUser");
        if (user == null) throw new ForbiddenException("Authentication required");

        ObjectId buyerId = user.getAccountType() == User.AccountType.ORGANIZATION ? user.getOrganizationId() : user.getId();
        
        Bson baseFilter = Filters.eq("subscriberId", buyerId);
        Bson searchFilter = MongoFilter.fromRequest(request, TransactionHistoryRequest.SEARCH_FIELDS);
        Bson finalFilter = MongoFilter.and(baseFilter, searchFilter);
        Bson sort = MongoFilter.sort(request);

        PanacheQuery<Transaction> query = transactionService.getTransactionHistory(finalFilter, sort, request.page(), request.limit());
        long total = transactionService.count(finalFilter);
        List<Transaction> transactions = query.list();

        // Mapping ke DTO dengan menyertakan data Subscription
        List<TransactionHistoryResponse> data = transactionService.buildHistoryResponse(transactions);
            
        PaginationMeta meta = PaginationMeta.of(request, total);

        return ResponseHelper.ok(data, "Riwayat transaksi berhasil diambil", meta);
    }

    @GET
    @Path("/status")
    @Operation(summary = "Cek status transaksi pending dan langganan aktif", description = "Digunakan untuk mendeteksi apakah user masih memiliki tagihan yang belum dibayar atau paket yang masih aktif")
    public Response getStatus(@Context ContainerRequestContext requestContext) {
        User user = (User) requestContext.getProperty("validatedUser");
        if (user == null) throw new ForbiddenException("Authentication required");

        ObjectId buyerId = user.getAccountType() == User.AccountType.ORGANIZATION ? user.getOrganizationId() : user.getId();

        TransactionStatusResponse statusResponse = transactionService.checkUserSubscriptionStatus(buyerId);

        return ResponseHelper.ok(statusResponse, "Status transaksi berhasil diambil");
    }

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

    @POST
    @Path("/{referenceId}/cancel")
    @Operation(summary = "Cancel pending transaction", description = "Membatalkan transaksi yang belum dibayar")
    public Response cancelTransaction(@PathParam("referenceId") String referenceId, @Context ContainerRequestContext requestContext) {
        User user = (User) requestContext.getProperty("validatedUser");
        if (user == null) throw new ForbiddenException("Authentication required");

        ObjectId buyerId = user.getAccountType() == User.AccountType.ORGANIZATION ? user.getOrganizationId() : user.getId();
        
        transactionService.cancelPendingTransaction(referenceId, buyerId);
        
        return ResponseHelper.ok(null, "Transaksi berhasil dibatalkan");
    }

    @POST
    @Path("/subscription/cancel")
    @Operation(summary = "Cancel active subscription", description = "Membatalkan langganan yang sedang aktif saat ini agar bisa membeli paket lain. Sisa hari akan hangus.")
    public Response cancelActiveSubscription(@Context ContainerRequestContext requestContext) {
        User user = (User) requestContext.getProperty("validatedUser");
        if (user == null) throw new ForbiddenException("Authentication required");

        ObjectId buyerId = user.getAccountType() == User.AccountType.ORGANIZATION ? user.getOrganizationId() : user.getId();
        
        transactionService.cancelActiveSubscription(buyerId);
        
        return ResponseHelper.ok(null, "Langganan aktif berhasil dibatalkan. Anda sekarang dapat membeli paket baru.");
    }

    @GET
    @Path("/admin/all")
    @RolesAllowed("SUPERADMIN")
    @Operation(summary = "Get all transactions (SUPERADMIN)", description = "Melihat semua riwayat transaksi dari seluruh user dengan paginasi, search, dan filter")
    public Response getAllTransactions(@BeanParam TransactionHistoryRequest request) {
        Bson filter = MongoFilter.fromRequest(request, TransactionHistoryRequest.SEARCH_FIELDS);
        Bson sort = MongoFilter.sort(request);

        PanacheQuery<Transaction> query = transactionService.getTransactionHistory(
            filter != null ? filter : new org.bson.Document(), sort, request.page(), request.limit());
        long total = transactionService.count(filter != null ? filter : new org.bson.Document());
        List<Transaction> transactions = query.list();

        // UBAH BARIS INI:
        List<AdminTransactionResponse> data = transactionService.buildAdminHistoryResponse(transactions);
        
        PaginationMeta meta = PaginationMeta.of(request, total);

        return ResponseHelper.ok(data, "Semua transaksi berhasil diambil", meta);
    }
}