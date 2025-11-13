package com.mss301.paymentservice.service;

import com.mss301.paymentservice.event.SubscriptionRequestEvent;
import com.mss301.paymentservice.event.SubscriptionResponseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for requesting subscription data via Kafka
 * Uses request-reply pattern with CompletableFuture
 */
@Service
@Slf4j
public class SubscriptionKafkaService {

    @Autowired
    private KafkaTemplate<String, SubscriptionRequestEvent> subscriptionRequestKafkaTemplate;

    @Value("${kafka.topic.subscription-request:subscription-request-topic}")
    private String subscriptionRequestTopic;

    @Value("${kafka.topic.subscription-response:subscription-response-topic}")
    private String subscriptionResponseTopic;

    // Store pending requests with their CompletableFuture
    private final Map<String, CompletableFuture<SubscriptionResponseEvent>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Request subscription data via Kafka
     * 
     * @param subscriptionId Subscription ID to fetch
     * @return SubscriptionResponseEvent with subscription data
     * @throws RuntimeException if timeout or error occurs
     */
    public SubscriptionResponseEvent getSubscription(Long subscriptionId) {
        String requestId = UUID.randomUUID().toString();
        log.info("Requesting subscription via Kafka - requestId: {}, subscriptionId: {}", requestId, subscriptionId);

        CompletableFuture<SubscriptionResponseEvent> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        try {
            // Send request
            SubscriptionRequestEvent request = SubscriptionRequestEvent.builder()
                    .requestId(requestId)
                    .subscriptionId(subscriptionId)
                    .replyTopic(subscriptionResponseTopic)
                    .build();

            subscriptionRequestKafkaTemplate.send(subscriptionRequestTopic, requestId, request);
            log.debug("Sent subscription request to Kafka - requestId: {}", requestId);

            // Wait for response with timeout (10 seconds)
            SubscriptionResponseEvent response = future.get(10, TimeUnit.SECONDS);

            if (!response.getSuccess()) {
                throw new RuntimeException("Failed to get subscription: " + response.getErrorMessage());
            }

            return response;

        } catch (Exception e) {
            log.error("Error requesting subscription {}: {}", subscriptionId, e.getMessage(), e);
            pendingRequests.remove(requestId);
            throw new RuntimeException("Failed to get subscription via Kafka: " + e.getMessage(), e);
        } finally {
            pendingRequests.remove(requestId);
        }
    }

    /**
     * Kafka listener for subscription responses
     */
    @KafkaListener(topics = "${kafka.topic.subscription-response:subscription-response-topic}", groupId = "payment-service-group", containerFactory = "subscriptionResponseKafkaListenerContainerFactory")
    public void handleSubscriptionResponse(SubscriptionResponseEvent response) {
        log.info("Received subscription response - requestId: {}, subscriptionId: {}, success: {}",
                response.getRequestId(), response.getSubscriptionId(), response.getSuccess());

        CompletableFuture<SubscriptionResponseEvent> future = pendingRequests.remove(response.getRequestId());
        if (future != null) {
            future.complete(response);
        } else {
            log.warn("Received response for unknown requestId: {}", response.getRequestId());
        }
    }
}
