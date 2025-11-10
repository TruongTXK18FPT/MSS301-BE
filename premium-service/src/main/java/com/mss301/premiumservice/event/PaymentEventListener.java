package com.mss301.premiumservice.event;

import com.mss301.premiumservice.model.Subscription;
import com.mss301.premiumservice.repository.SubscriptionRepository;
import com.mss301.premiumservice.constant.SubscriptionStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Slf4j
public class PaymentEventListener {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @EventListener
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent for subscription: {}",
                event.getSubscriptionId());

        try {
            Subscription subscription = subscriptionRepository
                    .findById(event.getSubscriptionId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Subscription not found: " + event.getSubscriptionId()));

            // ✅ Activate subscription
            subscription.setSubscriptionStatus(SubscriptionStatus.SUBSCRIBED);
            subscription.setStartDate(LocalDateTime.now());
            subscription.setUpdatedAt(LocalDateTime.now());

            subscriptionRepository.save(subscription);

            log.info("Subscription {} activated successfully", event.getSubscriptionId());
        } catch (Exception e) {
            log.error("Failed to activate subscription: {}", event.getSubscriptionId(), e);
            // TODO: Implement retry mechanism or dead letter queue
        }
    }
}