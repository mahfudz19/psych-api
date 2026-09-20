package com.psycorp.psychapi.feature.subscription.api;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import com.psycorp.psychapi.feature.subscription.api.dto.request.XenditInvoiceCallbackDto;
import com.psycorp.psychapi.feature.subscription.service.TransactionService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/webhooks/xendit")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Webhook", description = "Endpoint untuk menerima callback dari pihak ketiga")
public class WebhookResource {

    @Inject
    Logger log;

    @ConfigProperty(name = "xendit.webhook-token")
    String webhookToken;

    @Inject
    TransactionService transactionService;

    @POST
    @Path("/invoice")
    @Operation(summary = "Xendit Invoice Callback", description = "Menerima notifikasi status pembayaran dari Xendit.")
    @APIResponse(responseCode = "200", description = "Webhook diterima dan diproses")
    @APIResponse(responseCode = "403", description = "Token callback tidak valid", content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(type = SchemaType.STRING)))
    public Response handleInvoiceCallback(@HeaderParam("x-callback-token") String callbackToken, XenditInvoiceCallbackDto payload) {
        if (callbackToken == null || !callbackToken.equals(webhookToken)) {
            log.warn("Serangan Webhook terdeteksi: Token tidak valid!");
            return Response.status(Response.Status.FORBIDDEN).entity("Invalid Token").build();
        }

        log.infof("Menerima webhook Xendit untuk transaksi %s dengan status %s", payload.externalId, payload.status);

        // Panggil TransactionService untuk memproses pembayaran
        if (payload.status != null) {
            transactionService.processWebhookCallback(payload.externalId, payload.status);
        }

        return Response.ok().build();
    }
}