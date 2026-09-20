package com.psycorp.psychapi.feature.subscription.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.jboss.logging.Logger;

import com.psycorp.psychapi.feature.subscription.model.Subscription;
import com.psycorp.psychapi.feature.subscription.model.SubscriptionPlan;
import com.psycorp.psychapi.feature.subscription.model.Transaction;
import com.psycorp.psychapi.infrastructure.exception.NotFoundException;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class TransactionService implements PanacheMongoRepository<Transaction> {

    @Inject Logger log;
    @Inject PaymentService paymentService;
    @Inject SubscriptionPlanService planService;

    // Class pembungkus (Wrapper) agar kita bisa mengembalikan Transaction & Invoice sekaligus
    public record TransactionResult(Transaction transaction, Map<String, Object> invoiceData) {}

    @Transactional
    public TransactionResult checkout(ObjectId buyerId, String buyerEmail, String planCode, Subscription.SubscriberType subscriberType) {
        // 1. Validasi Paket
        SubscriptionPlan plan = planService.find("code", planCode).firstResult();
        if (plan == null) {
            throw new ValidationException("PLAN_NOT_FOUND", "Paket tidak ditemukan");
        }

        // 2. Generate Reference ID Unik
        String referenceId = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 3. Buat Tagihan di Xendit
        String description = "Pembelian paket: " + plan.getName();
        Map<String, Object> xenditInvoice = paymentService.createInvoiceRaw(referenceId, plan.getPrice(), buyerEmail, description);

        // 4. Catat Transaksi di DB
        Transaction transaction = new Transaction();
        transaction.setReferenceId(referenceId);
        transaction.setXenditInvoiceId((String) xenditInvoice.get("id"));
        transaction.setCheckoutUrl((String) xenditInvoice.get("invoice_url"));
        transaction.setSubscriberId(buyerId);
        transaction.setSubscriberType(subscriberType);
        transaction.setPlanId(plan.getId());
        transaction.setAmount(plan.getPrice());
        transaction.setStatus(Transaction.Status.PENDING);
        transaction.setCreatedAt(Instant.now());
        transaction.setExpiredAt(Instant.now().plus(24, ChronoUnit.HOURS));
        
        transaction.persist();

        return new TransactionResult(transaction, xenditInvoice);
    }

    public TransactionResult getTransactionDetail(String referenceId, ObjectId buyerId) {
        Transaction transaction = find("referenceId", referenceId).firstResult();
        if (transaction == null) {
            throw new NotFoundException("TX_NOT_FOUND", "Transaksi tidak ditemukan");
        }
        
        // Proteksi Otorisasi (Hanya yang beli yang boleh lihat)
        if (!transaction.getSubscriberId().equals(buyerId)) {
            throw new ValidationException("FORBIDDEN", "Anda tidak memiliki akses ke transaksi ini");
        }

        // Tarik data metode pembayaran real-time dari Xendit
        Map<String, Object> xenditInvoice = paymentService.getInvoiceRaw(transaction.getXenditInvoiceId());

        return new TransactionResult(transaction, xenditInvoice);
    }

    @Transactional
    public void processWebhookCallback(String externalId, Transaction.Status newStatus) {
        Transaction transaction = find("referenceId", externalId).firstResult();
        if (transaction == null) return; 

        if (transaction.getStatus() == Transaction.Status.PAID) return; // Idempotency

        transaction.setStatus(newStatus);
        if (newStatus == Transaction.Status.PAID) {
            transaction.setPaidAt(Instant.now());
            
            // FASE 4: Melahirkan Subscription
            SubscriptionPlan plan = planService.findById(transaction.getPlanId());
            if (plan != null) {
                Subscription subscription = new Subscription();
                subscription.setPlanId(plan.getId());
                subscription.setSubscriberId(transaction.getSubscriberId());
                subscription.setSubscriberType(transaction.getSubscriberType());
                subscription.setStartDate(Instant.now());
                subscription.setEndDate(Instant.now().plus(plan.getDurationDays(), ChronoUnit.DAYS));
                subscription.setStatus(Subscription.Status.ACTIVE);
                subscription.setPaymentGatewayId(transaction.getXenditInvoiceId());
                subscription.setCreatedAt(Instant.now());
                subscription.setUpdatedAt(Instant.now());
                
                subscription.persist();
                log.infof("✅ Langganan aktif untuk %s (%s)", transaction.getSubscriberId(), transaction.getSubscriberType());
            }
            
            log.infof("✅ Transaksi %s BERHASIL.", externalId);
        }

        transaction.update();
    }
}