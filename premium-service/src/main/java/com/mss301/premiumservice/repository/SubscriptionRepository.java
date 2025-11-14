package com.mss301.premiumservice.repository;

import com.mss301.premiumservice.constant.SubscriptionStatus;
import com.mss301.premiumservice.model.Subscription;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
   @EntityGraph(attributePaths = { "plan", "plan.entitlements" })
   Optional<Subscription> findBySubscriptionId(Long subscriptionId);

   @EntityGraph(attributePaths = { "plan", "plan.entitlements" })
   List<Subscription> findByUserId(Long userId);

   @EntityGraph(attributePaths = { "plan", "plan.entitlements" })
   @Override
   Optional<Subscription> findById(Long id);

   /**
    * Find current active subscription for a user
    * Active = SUBSCRIBED status and endDate > now
    */
   @EntityGraph(attributePaths = { "plan", "plan.entitlements" })
   @Query("SELECT s FROM Subscription s WHERE s.userId = :userId " +
         "AND s.subscriptionStatus = :status " +
         "AND s.endDate > :now " +
         "ORDER BY s.endDate DESC")
   Optional<Subscription> findCurrentActiveSubscription(
         @Param("userId") Long userId,
         @Param("status") SubscriptionStatus status,
         @Param("now") LocalDateTime now);

   /**
    * Find all subscriptions for a user, ordered by creation date descending
    */
   @EntityGraph(attributePaths = { "plan", "plan.entitlements" })
   @Query("SELECT s FROM Subscription s WHERE s.userId = :userId " +
         "ORDER BY s.createdAt DESC")
   List<Subscription> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
