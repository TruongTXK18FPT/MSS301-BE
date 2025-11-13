package com.mss301.premiumservice.listener;

import com.mss301.premiumservice.event.SubscriptionRequestEvent;
import com.mss301.premiumservice.event.SubscriptionResponseEvent;
import com.mss301.premiumservice.service.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for subscription request events from payment-service
 * Responds with subscription data via Kafka
 */
@Component
@Slf4j
public class SubscriptionRequestListener {

        @Autowired
        private SubscriptionService subscriptionService;

        @Autowired
        private KafkaTemplate<String, SubscriptionResponseEvent> subscriptionResponseKafkaTemplate;

        @Value("${kafka.topic.subscription-response:subscription-response-topic}")
        private String subscriptionResponseTopic;

        @KafkaListener(topics = "${kafka.topic.subscription-request:subscription-request-topic}", groupId = "premium-service-group", containerFactory = "subscriptionRequestKafkaListenerContainerFactory")
        public void handleSubscriptionRequest(SubscriptionRequestEvent request) {
                log.info("Received subscription request - requestId: {}, subscriptionId: {}",
                                request.getRequestId(), request.getSubscriptionId());

                try {
                        // Get subscription data
                        var subscriptionResponse = subscriptionService
                                        .findBySubscriptionId(request.getSubscriptionId());

                        if (subscriptionResponse == null) {
                                // Send error response
                                SubscriptionResponseEvent errorResponse = SubscriptionResponseEvent.builder()
                                                .requestId(request.getRequestId())
                                                .subscriptionId(request.getSubscriptionId())
                                                .success(false)
                                                .errorMessage("Subscription not found: " + request.getSubscriptionId())
                                                .build();

                                subscriptionResponseKafkaTemplate.send(
                                                request.getReplyTopic() != null ? request.getReplyTopic()
                                                                : subscriptionResponseTopic,
                                                request.getRequestId(),
                                                errorResponse);

                                log.error("Subscription not found: {}", request.getSubscriptionId());
                                return;
                        }

                        // Send success response
                        // Parse userId from String id if available
                        Long userId = null;
                        if (subscriptionResponse.getUser() != null && subscriptionResponse.getUser().getId() != null) {
                                try {
                                        userId = Long.parseLong(subscriptionResponse.getUser().getId());
                                } catch (NumberFormatException e) {
                                        log.warn("Failed to parse userId from string: {}",
                                                        subscriptionResponse.getUser().getId());
                                }
                        }

                        SubscriptionResponseEvent response = SubscriptionResponseEvent.builder()
                                        .requestId(request.getRequestId())
                                        .subscriptionId(subscriptionResponse.getSubscriptionId())
                                        .userId(userId)
                                        .planId(subscriptionResponse.getPlan() != null
                                                        ? subscriptionResponse.getPlan().getPlanId()
                                                        : null)
                                        .planPrice(
                                                        subscriptionResponse.getPlan() != null
                                                                        ? subscriptionResponse.getPlan().getPrice()
                                                                        : null)
                                        .subscriptionStatus(subscriptionResponse.getSubscriptionStatus() != null
                                                        ? subscriptionResponse.getSubscriptionStatus().name()
                                                        : null)
                                        .success(true)
                                        .build();

                        subscriptionResponseKafkaTemplate.send(
                                        request.getReplyTopic() != null ? request.getReplyTopic()
                                                        : subscriptionResponseTopic,
                                        request.getRequestId(),
                                        response);

                        log.info("Sent subscription response - requestId: {}, subscriptionId: {}, planPrice: {}",
                                        request.getRequestId(), subscriptionResponse.getSubscriptionId(),
                                        response.getPlanPrice());

                } catch (Exception e) {
                        log.error("Error processing subscription request {}: {}", request.getRequestId(),
                                        e.getMessage(), e);

                        // Send error response
                        SubscriptionResponseEvent errorResponse = SubscriptionResponseEvent.builder()
                                        .requestId(request.getRequestId())
                                        .subscriptionId(request.getSubscriptionId())
                                        .success(false)
                                        .errorMessage("Error processing request: " + e.getMessage())
                                        .build();

                        subscriptionResponseKafkaTemplate.send(
                                        request.getReplyTopic() != null ? request.getReplyTopic()
                                                        : subscriptionResponseTopic,
                                        request.getRequestId(),
                                        errorResponse);
                }
        }
}
