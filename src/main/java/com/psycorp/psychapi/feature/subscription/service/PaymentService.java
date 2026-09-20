package com.psycorp.psychapi.feature.subscription.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PaymentService {

    @Inject
    Logger log;

    @Inject
    ObjectMapper objectMapper; // Bawaan Quarkus Jackson

    @ConfigProperty(name = "xendit.api-key")
    String xenditApiKey;

    private HttpClient httpClient;
    private String authHeader;

    @PostConstruct
    public void init() {
        this.httpClient = HttpClient.newHttpClient();
        // Xendit menggunakan Basic Auth (API Key sebagai username, password kosong)
        String auth = xenditApiKey + ":";
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        log.infof("Custom Xendit HTTP Client terhubung. Mode: %s", xenditApiKey);
    }

    public Map<String, Object> createInvoiceRaw(String referenceId, Double amount, String customerEmail, String description) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("external_id", referenceId);
            params.put("amount", amount);
            params.put("description", description);
            params.put("payer_email", customerEmail);
            params.put("invoice_duration", 86400);

            String requestBody = objectMapper.writeValueAsString(params);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.xendit.co/v2/invoices"))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.errorf("Xendit Error: %s", response.body());
                throw new RuntimeException("Gagal membuat tagihan di payment gateway.");
            }

            return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});

        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Error saat memanggil API Xendit", e);
            throw new RuntimeException("Layanan pembayaran sedang gangguan. Coba beberapa saat lagi.");
        }
    }

    public Map<String, Object> getInvoiceRaw(String invoiceId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.xendit.co/v2/invoices/" + invoiceId))
                .header("Authorization", authHeader)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Gagal mengambil tagihan Xendit", e);
            throw new RuntimeException("Layanan pembayaran sedang gangguan.");
        }
    }
}