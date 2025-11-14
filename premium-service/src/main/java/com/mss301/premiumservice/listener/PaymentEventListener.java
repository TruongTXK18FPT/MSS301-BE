package com.mss301.premiumservice.listener;

import com.mss301.premiumservice.constant.SubscriptionStatus;
import com.mss301.premiumservice.model.Subscription;
import com.mss301.premiumservice.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Kafka consumer for payment events
 * Activates subscriptions when payment completes
 */
@Component
@Slf4j
public class PaymentEventListener {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @KafkaListener(topics = "${kafka.topic.payment-completed:payment-completed-topic}", groupId = "premium-service-group", containerFactory = "paymentCompletedKafkaListenerContainerFactory")
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received payment completed event - orderId: {}, subscriptionId: {}, userId: {}",
                event.getOrderId(), event.getSubscriptionId(), event.getUserId());

        try {
            // Use findBySubscriptionId to ensure plan is eagerly loaded
            Subscription subscription = subscriptionRepository
                    .findBySubscriptionId(event.getSubscriptionId())
                    .orElseThrow(() -> new RuntimeException("Subscription not found: " + event.getSubscriptionId()));

            // Verify subscription is in PENDING_PAYMENT status
            if (subscription.getSubscriptionStatus() != SubscriptionStatus.PENDING_PAYMENT) {
                log.warn("Subscription {} is not in PENDING_PAYMENT status. Current status: {}. Skipping activation.",
                        event.getSubscriptionId(), subscription.getSubscriptionStatus());
                return;
            }

            // Activate subscription
            subscription.setSubscriptionStatus(SubscriptionStatus.SUBSCRIBED);
            LocalDateTime now = LocalDateTime.now();
            subscription.setStartDate(now);

            // Calculate end date based on billing cycle
            Integer billingCycle = subscription.getPlan() != null ? subscription.getPlan().getBillingCycle() : 1;
            LocalDateTime endDate = calculateEndDate(now, billingCycle);
            subscription.setEndDate(endDate);

            subscriptionRepository.save(subscription);

            log.info("Subscription {} activated for user {} - Start: {}, End: {}, Plan: {}",
                    event.getSubscriptionId(),
                    event.getUserId(),
                    subscription.getStartDate(),
                    subscription.getEndDate(),
                    subscription.getPlan() != null ? subscription.getPlan().getName() : "Unknown");
        } catch (Exception e) {
            log.error("Failed to activate subscription {}: {}", event.getSubscriptionId(), e.getMessage(), e);
            throw e; // Re-throw to trigger Kafka retry mechanism
        }
    }

    private LocalDateTime calculateEndDate(LocalDateTime startDate, Integer billingCycleMonths) {
        if (billingCycleMonths == null || billingCycleMonths <= 0) {
            billingCycleMonths = 1; // Default to 1 month
        }
        return startDate.plusMonths(billingCycleMonths);
    }
}