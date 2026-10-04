package com.psycorp.psychapi.feature.subscription.service;

import java.time.Instant;

import org.bson.types.ObjectId;

import com.psycorp.psychapi.feature.auth.api.dto.response.UserInfoResponse;
import com.psycorp.psychapi.feature.subscription.models.Subscription;
import com.psycorp.psychapi.feature.user.models.User;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SubscriptionService implements PanacheMongoRepository<Subscription> {
    public Subscription getActiveSubscription(User user) {
        if (user == null) return null;

        // Polimorfisme: Tentukan siapa pembelinya (USER vs ORGANIZATION)
        ObjectId subscriberId = user.getAccountType() == User.AccountType.ORGANIZATION ? 
            user.getOrganizationId() : user.getId();

        if (subscriberId == null) return null;

        // Query: Cari subscription ACTIVE yang endDate-nya belum lewat
        return find(
            "subscriberId = ?1 and status = ?2 and endDate > ?3", 
            subscriberId, Subscription.Status.ACTIVE, Instant.now()
        ).firstResult();
    }

    public UserInfoResponse.SubscriptionInfo getSubInfo(User user) {
        Subscription activeSub = this.getActiveSubscription(user);

        UserInfoResponse.SubscriptionInfo subInfo = null;
        if (activeSub != null) {
            subInfo = new UserInfoResponse.SubscriptionInfo(
                true,
                activeSub.getPlanId().toHexString(),
                activeSub.getStartDate(),
                activeSub.getEndDate()
            );
        }

        return subInfo;
    }
}
