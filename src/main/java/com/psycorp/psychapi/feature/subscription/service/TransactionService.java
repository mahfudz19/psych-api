package com.psycorp.psychapi.feature.subscription.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;

import com.psycorp.psychapi.feature.subscription.api.dto.response.AdminTransactionResponse;
import com.psycorp.psychapi.feature.subscription.api.dto.response.SubscriptionPlanResponse;
import com.psycorp.psychapi.feature.subscription.api.dto.response.TransactionHistoryResponse;
import com.psycorp.psychapi.feature.subscription.api.dto.response.TransactionStatusResponse;
import com.psycorp.psychapi.feature.subscription.models.Subscription;
import com.psycorp.psychapi.feature.subscription.models.SubscriptionPlan;
import com.psycorp.psychapi.feature.subscription.models.Transaction;
import com.psycorp.psychapi.infrastructure.exception.NotFoundException;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.mongodb.panache.PanacheQuery;
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
        // 1. CEK TRANSAKSI PENDING
        Transaction existingPending = find("subscriberId = ?1 and status = ?2", buyerId, Transaction.Status.PENDING).firstResult();
        if (existingPending != null) {
            throw new ValidationException("PENDING_TRANSACTION_EXISTS", "Anda masih memiliki tagihan yang belum dibayar. Silakan bayar atau batalkan tagihan tersebut terlebih dahulu.");
        }

        // 2. Validasi Paket
        SubscriptionPlan plan = planService.find("code", planCode).firstResult();
        if (plan == null) {
            throw new ValidationException("PLAN_NOT_FOUND", "Paket tidak ditemukan");
        }

        // 3. CEK LANGGANAN AKTIF (Aturan MVP: Cancel-then-Resubscribe)
        Subscription activeSubscription = Subscription.find(
            "subscriberId = ?1 and status = ?2 and endDate > ?3", 
            buyerId, Subscription.Status.ACTIVE, Instant.now()
        ).firstResult();
        
        if (activeSubscription != null) {
            throw new ValidationException("ACTIVE_SUBSCRIPTION_EXISTS", "Anda masih memiliki paket langganan yang aktif. Silakan batalkan langganan saat ini terlebih dahulu jika ingin mengganti paket.");
        }

        // 4. Generate Reference ID Unik
        String referenceId = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 5. Buat Tagihan di Xendit
        String description = "Pembelian paket: " + plan.getName();
        Map<String, Object> xenditInvoice = paymentService.createInvoiceRaw(referenceId, plan.getPrice(), buyerEmail, description);

        // 6. Catat Transaksi di DB
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

    public PanacheQuery<Transaction> getTransactionHistory(Bson filter, Bson sort, int page, int limit) {
        return find(filter, sort).page(page - 1, limit);
    }

    @Transactional
    public void cancelPendingTransaction(String referenceId, ObjectId buyerId) {
        Transaction transaction = find("referenceId", referenceId).firstResult();
        if (transaction == null) {
            throw new NotFoundException("TX_NOT_FOUND", "Transaksi tidak ditemukan");
        }
        
        if (!transaction.getSubscriberId().equals(buyerId)) {
            throw new ValidationException("FORBIDDEN", "Anda tidak memiliki akses ke transaksi ini");
        }

        if (transaction.getStatus() != Transaction.Status.PENDING) {
            throw new ValidationException("INVALID_STATUS", "Hanya transaksi berstatus PENDING yang dapat dibatalkan");
        }

        // Ubah status di database kita
        transaction.setStatus(Transaction.Status.FAILED);
        transaction.update();

        paymentService.expireInvoice(transaction.getXenditInvoiceId());
    }

    public TransactionStatusResponse checkUserSubscriptionStatus(ObjectId buyerId) {
        // Cek Langganan Aktif
        Subscription activeSubscription = Subscription.find(
            "subscriberId = ?1 and status = ?2 and endDate > ?3", 
            buyerId, Subscription.Status.ACTIVE, Instant.now()
        ).firstResult();
        
        // Cek Transaksi Pending
        Transaction pendingTransaction = find(
            "subscriberId = ?1 and status = ?2", 
            buyerId, Transaction.Status.PENDING
        ).firstResult();

        TransactionStatusResponse.ActiveSubscriptionDetail activeSubDetail = null;
        if (activeSubscription != null && activeSubscription.getPlanId() != null) {
            SubscriptionPlan plan = planService.findById(activeSubscription.getPlanId());
            if (plan != null) {
                activeSubDetail = new TransactionStatusResponse.ActiveSubscriptionDetail(
                    SubscriptionPlanResponse.fromEntity(plan),
                    activeSubscription.getStartDate(),
                    activeSubscription.getEndDate()
                );
            }
        }

        return new TransactionStatusResponse(
            activeSubscription != null,
            pendingTransaction != null,
            pendingTransaction != null ? pendingTransaction.getReferenceId() : null,
            activeSubDetail
        );
    }

    @Transactional
    public void cancelActiveSubscription(ObjectId buyerId) {
        Subscription activeSubscription = Subscription.find(
            "subscriberId = ?1 and status = ?2 and endDate > ?3", 
            buyerId, Subscription.Status.ACTIVE, Instant.now()
        ).firstResult();

        if (activeSubscription == null) {
            throw new ValidationException("NO_ACTIVE_SUBSCRIPTION", "Tidak ada langganan aktif untuk dibatalkan.");
        }

        // Langsung matikan akses (Sisa hari hangus) agar user bisa langsung beli paket baru
        activeSubscription.setStatus(Subscription.Status.CANCELED);
        activeSubscription.setCanceledAt(Instant.now());
        activeSubscription.setUpdatedAt(Instant.now());
        activeSubscription.update();
    }

    public List<TransactionHistoryResponse> buildHistoryResponse(List<Transaction> transactions) {
        List<String> invoiceIds = transactions.stream()
            .filter(t -> t.getStatus() == Transaction.Status.PAID && t.getXenditInvoiceId() != null)
            .map(t -> t.getXenditInvoiceId())
            .toList();

        java.util.Map<String, Subscription> subscriptionMap = new java.util.HashMap<>();
        if (!invoiceIds.isEmpty()) {
            List<Subscription> subs = Subscription.list("paymentGatewayId in ?1", invoiceIds);
            for (Subscription sub : subs) {
                subscriptionMap.put(sub.getPaymentGatewayId(), sub);
            }
        }

        return transactions.stream()
            .map(t -> TransactionHistoryResponse.fromEntity(t, subscriptionMap.get(t.getXenditInvoiceId())))
            .toList();
    }

    public List<AdminTransactionResponse> buildAdminHistoryResponse(List<Transaction> transactions) {
        if (transactions.isEmpty()) return List.of();

        // 1. Kumpulkan ID untuk batch query
        List<String> invoiceIds = transactions.stream()
            .filter(t -> t.getXenditInvoiceId() != null)
            .map(t -> t.getXenditInvoiceId())
            .toList();
            
        List<ObjectId> planIds = transactions.stream()
            .map(t -> t.getPlanId())
            .filter(id -> id != null)
            .distinct()
            .toList();

        List<ObjectId> subscriberIds = transactions.stream()
            .map(t -> t.getSubscriberId())
            .filter(id -> id != null)
            .distinct()
            .toList();

        // 2. Batch Query Data Terkait
        java.util.Map<String, Subscription> subMap = invoiceIds.isEmpty() ? java.util.Map.of() : 
            Subscription.<Subscription>list("paymentGatewayId in ?1", invoiceIds).stream()
                .collect(java.util.stream.Collectors.toMap(s -> s.getPaymentGatewayId(), s -> s));

        java.util.Map<ObjectId, SubscriptionPlan> planMap = planIds.isEmpty() ? java.util.Map.of() :
            SubscriptionPlan.<SubscriptionPlan>list("_id in ?1", planIds).stream()
                .collect(java.util.stream.Collectors.toMap(p -> p.getId(), p -> p));

        // Untuk subscriber, kita cari di User
        java.util.Map<ObjectId, com.psycorp.psychapi.feature.user.models.User> userMap = 
            com.psycorp.psychapi.feature.user.models.User.<com.psycorp.psychapi.feature.user.models.User>list("_id in ?1", subscriberIds).stream()
                .collect(java.util.stream.Collectors.toMap(u -> u.getId(), u -> u));

        // 3. Mapping ke DTO
        return transactions.stream().map(t -> {
            Subscription sub = subMap.get(t.getXenditInvoiceId());
            SubscriptionPlan plan = planMap.get(t.getPlanId());
            
            String subEmail = null;
            String subName = null;
            
            if (t.getSubscriberType() == Subscription.SubscriberType.USER) {
                com.psycorp.psychapi.feature.user.models.User u = userMap.get(t.getSubscriberId());
                if (u != null) {
                    subEmail = u.getEmail();
                    subName = u.getFullName();
                }
            } else {
                // Fallback untuk Organization jika belum ada map-nya
                subName = "Organization " + t.getSubscriberId().toHexString();
            }

            AdminTransactionResponse.SubscriptionDetail subDetail = null;
            if (sub != null) {
                subDetail = new AdminTransactionResponse.SubscriptionDetail(
                    sub.getId().toHexString(),
                    sub.getStatus(),
                    sub.getStartDate(),
                    sub.getEndDate(),
                    sub.getCanceledAt()
                );
            }

            return new AdminTransactionResponse(
                t.getSubscriberId() != null ? t.getSubscriberId().toHexString() : null,
                t.getSubscriberType(),
                subEmail,
                subName,
                t.getPlanId() != null ? t.getPlanId().toHexString() : null,
                plan != null ? plan.getCode() : null,
                plan != null ? plan.getName() : null,
                t.getAmount(),
                "IDR",
                t.getReferenceId(),
                t.getXenditInvoiceId(),
                t.getCheckoutUrl(),
                t.getPaymentMethod(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getPaidAt(),
                t.getExpiredAt(),
                subDetail
            );
        }).toList();
    }
}