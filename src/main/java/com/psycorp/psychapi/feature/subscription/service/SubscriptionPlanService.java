package com.psycorp.psychapi.feature.subscription.service;

import java.time.Instant;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.feature.subscription.model.SubscriptionPlan;
import com.psycorp.psychapi.infrastructure.exception.ValidationException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SubscriptionPlanService implements PanacheMongoRepository<SubscriptionPlan> {

    @Transactional
    public SubscriptionPlan create(String name, String code, Double price, Integer durationDays, SubscriptionPlan.TargetAudience targetAudience, Integer maxSeats) {
        if (find("code", code).firstResult() != null) {
            throw new ValidationException("PLAN_CODE_EXISTS", "Kode plan '" + code + "' sudah digunakan");
        }

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName(name);
        plan.setCode(code);
        plan.setPrice(price);
        plan.setDurationDays(durationDays);
        plan.setTargetAudience(targetAudience);
        plan.setMaxSeats(maxSeats);
        plan.setCreatedAt(Instant.now());
        plan.setUpdatedAt(Instant.now());
        
        plan.persist();
        return plan;
    }

    @Transactional
    public SubscriptionPlan update(ObjectId id, String name, String code, Double price, Integer durationDays, SubscriptionPlan.TargetAudience targetAudience, Integer maxSeats) {
        SubscriptionPlan plan = findById(id);
        if (plan == null || plan.getDeletedAt() != null) {
            throw new ValidationException("PLAN_NOT_FOUND", "Paket tidak ditemukan");
        }

        if (code != null && !code.equals(plan.getCode())) {
            SubscriptionPlan existing = find("code", code).firstResult();
            if (existing != null && !existing.getId().equals(id)) {
                throw new ValidationException("PLAN_CODE_EXISTS", "Kode plan '" + code + "' sudah digunakan");
            }
            plan.setCode(code);
        }

        if (name != null) plan.setName(name);
        if (price != null) plan.setPrice(price);
        if (durationDays != null) plan.setDurationDays(durationDays);
        if (targetAudience != null) plan.setTargetAudience(targetAudience);
        if (maxSeats != null) plan.setMaxSeats(maxSeats);
        plan.setUpdatedAt(Instant.now());

        plan.update();
        return plan;
    }

    @Transactional
    public void softDelete(ObjectId id) {
        SubscriptionPlan plan = findById(id);
        if (plan == null || plan.getDeletedAt() != null) {
            throw new ValidationException("PLAN_NOT_FOUND", "Paket tidak ditemukan");
        }

        plan.setDeletedAt(Instant.now());
        plan.setUpdatedAt(Instant.now());
        plan.update();
    }
}

