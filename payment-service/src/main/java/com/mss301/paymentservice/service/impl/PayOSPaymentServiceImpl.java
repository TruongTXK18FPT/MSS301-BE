package com.mss301.paymentservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mss301.paymentservice.client.PayOSClient;
import com.mss301.paymentservice.config.PayOSConfig;
import com.mss301.paymentservice.service.SubscriptionKafkaService;
import com.mss301.paymentservice.constant.Status;
import com.mss301.paymentservice.event.PaymentCompletedEvent;
import com.mss301.paymentservice.event.PaymentCreatedEvent;
import com.mss301.paymentservice.event.SubscriptionResponseEvent;
import com.mss301.paymentservice.model.PaymentCommand;
import com.mss301.paymentservice.model.PaymentHistory;
import com.mss301.paymentservice.model.PaymentWebhookLog;
import com.mss301.paymentservice.model.dtos.request.PaymentRequest;
import com.mss301.paymentservice.model.dtos.payos.PayOSWebhookRequest;
import com.mss301.paymentservice.model.dtos.payos.PayOSWebhookData;
import com.mss301.paymentservice.model.dtos.response.PaymentResponse;
import com.mss301.paymentservice.model.dtos.payos.PayOSPaymentData;
import com.mss301.paymentservice.model.dtos.payos.PayOSResponse;
import com.mss301.paymentservice.repository.PaymentCommandRepository;
import com.mss301.paymentservice.repository.PaymentHistoryRepository;
import com.mss301.paymentservice.repository.PaymentWebhookLogRepository;
import com.mss301.paymentservice.service.PaymentWriteService;
import com.mss301.paymentservice.util.PayOSSignatureUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * PayOS-based payment service implementation
 */
@Service("payosPaymentService")
@Transactional
@Slf4j
public class PayOSPaymentServiceImpl implements PaymentWriteService {

    @Autowired
    private PaymentCommandRepository commandRepository;

    @Autowired
    private PaymentHistoryRepository historyRepository;

    @Autowired
    private PaymentWebhookLogRepository webhookLogRepository;

    @Autowired
    private PayOSClient payOSClient;

    @Autowired
    private SubscriptionKafkaService subscriptionKafkaService;

    @Autowired
    private PayOSConfig payOSConfig;

    @Autowired
    private PayOSSignatureUtil signatureUtil;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private KafkaTemplate<String, PaymentCompletedEvent> paymentCompletedKafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${kafka.topic.payment-completed:payment-completed-topic}")
    private String paymentCompletedTopic;

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("Creating PayOS payment - userId: {}, subscriptionId: {}, planId: {}, amount: {}",
                request.getUserId(), request.getSubscriptionId(), request.getPlanId(), request.getAmount());

        // 1. Validate subscription via Kafka event
        SubscriptionResponseEvent subscriptionEvent = subscriptionKafkaService
                .getSubscription(request.getSubscriptionId());

        if (subscriptionEvent == null || !subscriptionEvent.getSuccess()) {
            String errorMsg = subscriptionEvent != null ? subscriptionEvent.getErrorMessage()
                    : "Subscription not found";
            throw new RuntimeException("Subscription not found: " + request.getSubscriptionId() + " - " + errorMsg);
        }

        // 2. Validate amount matches plan price
        if (!request.getAmount().equals(subscriptionEvent.getPlanPrice())) {
            throw new RuntimeException("Amount mismatch: expected " +
                    subscriptionEvent.getPlanPrice() + ", got " + request.getAmount());
        }

        // 3. Check no pending payment exists
        if (commandRepository.existsBySubscriptionIdAndStatus(request.getSubscriptionId(), Status.PENDING)) {
            throw new RuntimeException(
                    "Pending payment already exists for subscription: " + request.getSubscriptionId());
        }

        // 4. Create PaymentCommand
        PaymentCommand payment = new PaymentCommand();
        payment.setUserId(request.getUserId());
        payment.setSubscriptionId(request.getSubscriptionId());
        payment.setPlanId(request.getPlanId());
        payment.setAmount(request.getAmount());
        payment.setOrderInfo(request.getOrderInfo());
        payment.setStatus(Status.PENDING);

        PaymentCommand saved = commandRepository.save(payment);

        // 5. Save history
        saveHistory(saved.getOrderId(), null, Status.PENDING, "Payment created");

        // 6. Call PayOS to create payment link
        try {
            Long orderCode = System.currentTimeMillis();
            // Store PayOS orderCode in return URL for easy lookup
            String returnUrl = payOSConfig.getReturnUrl() + "?orderCode=" + orderCode + "&id=" + saved.getOrderId();
            String cancelUrl = payOSConfig.getCancelUrl() + "?orderCode=" + orderCode + "&id=" + saved.getOrderId();

            log.info("Creating PayOS payment link - orderCode: {}, returnUrl: {}, cancelUrl: {}", orderCode, returnUrl,
                    cancelUrl);

            PayOSResponse<PayOSPaymentData> payosResponse = payOSClient.createPaymentLink(
                    orderCode,
                    request.getAmount(),
                    request.getOrderInfo(),
                    returnUrl,
                    cancelUrl);

            // 7. Update payment with PayOS data
            saved.setPayosOrderCode(orderCode); // Store PayOS orderCode for lookup
            saved.setPayosPaymentLinkId(payosResponse.getData().getPaymentLinkId());
            saved.setPaymentUrl(payosResponse.getData().getCheckoutUrl());
            saved.setStatus(Status.PROCESSING);
            commandRepository.save(saved);

            saveHistory(saved.getOrderId(), Status.PENDING, Status.PROCESSING, "PayOS link created");

            log.info("PayOS payment link created - orderId: {}, paymentLinkId: {}",
                    saved.getOrderId(), saved.getPayosPaymentLinkId());

        } catch (Exception e) {
            log.error("Failed to create PayOS payment link", e);
            saved.setStatus(Status.FAILED);
            commandRepository.save(saved);
            saveHistory(saved.getOrderId(), Status.PENDING, Status.FAILED,
                    "Failed to create PayOS link: " + e.getMessage());
            throw new RuntimeException("Failed to create payment link", e);
        }

        // 8. Sync to MongoDB (CQRS)
        eventPublisher.publishEvent(new PaymentCreatedEvent(saved));

        return convertToResponse(saved);
    }

    @Override
    public PaymentResponse processPaymentCallback(Map<String, String> params) {
        log.info("Processing PayOS payment callback - params: {}", params);

        // For now, delegate to processWebhook
        // In production, you may parse params into PayOSWebhookRequest
        throw new UnsupportedOperationException("Use processWebhook method instead");
    }

    public void processWebhook(PayOSWebhookRequest webhook) {
        log.info("Processing PayOS webhook - paymentLinkId: {}", webhook.getData().getPaymentLinkId());

        try {
            // 1. Generate webhook ID for idempotency
            String webhookId = signatureUtil.generateWebhookId(
                    webhook.getData().getPaymentLinkId(),
                    webhook.getSignature());

            // 2. Check if already processed
            if (webhookLogRepository.existsByWebhookId(webhookId)) {
                log.warn("Webhook already processed: {}", webhookId);
                return;
            }

            // 3. Verify signature (skip for return URL processing)
            // Return URL processing uses a mock signature, so we skip verification
            if (!"return-url-signature".equals(webhook.getSignature())) {
                if (!signatureUtil.verifyWebhookSignature(
                        webhook.getSignature(),
                        webhook.getData(),
                        payOSConfig.getChecksumKey())) {
                    throw new RuntimeException("Invalid webhook signature");
                }
            } else {
                log.info("Skipping signature verification for return URL processing");
            }

            // 4. Find payment by paymentLinkId FIRST (needed for orderId in webhook log)
            PaymentCommand payment = commandRepository
                    .findByPayosPaymentLinkId(webhook.getData().getPaymentLinkId())
                    .orElseThrow(() -> new RuntimeException("Payment not found for paymentLinkId: " +
                            webhook.getData().getPaymentLinkId()));

            // 5. Save webhook log WITH orderId
            PaymentWebhookLog webhookLog = PaymentWebhookLog.builder()
                    .webhookId(webhookId)
                    .orderId(payment.getOrderId()) // Set orderId before saving
                    .signature(webhook.getSignature())
                    .rawPayload(objectMapper.writeValueAsString(webhook))
                    .processed(false)
                    .build();
            webhookLogRepository.save(webhookLog);

            // 5.1. Check if payment is already SUCCESS - skip processing to avoid duplicate
            if (payment.getStatus() == Status.SUCCESS) {
                log.warn(
                        "Payment {} is already SUCCESS. Skipping webhook processing to avoid duplicate. Webhook ID: {}",
                        payment.getOrderId(), webhookId);
                // Mark webhook as processed but skip business logic
                webhookLog.setProcessed(true);
                webhookLog.setProcessedAt(LocalDateTime.now());
                webhookLog.setProcessingError("Payment already SUCCESS - skipped duplicate processing");
                webhookLogRepository.save(webhookLog);
                return;
            }

            // 6. Update payment based on webhook status
            Status oldStatus = payment.getStatus();
            Status newStatus = mapPayOSStatus(webhook.getData().getCode());

            // 6.1. If new status is same as old status, skip update (idempotency)
            if (oldStatus == newStatus) {
                log.info("Payment {} status unchanged ({}). Skipping update. Webhook ID: {}",
                        payment.getOrderId(), oldStatus, webhookId);
                webhookLog.setProcessed(true);
                webhookLog.setProcessedAt(LocalDateTime.now());
                webhookLogRepository.save(webhookLog);
                return;
            }

            payment.setStatus(newStatus);
            // Update PayOS orderCode from webhook if not set
            if (payment.getPayosOrderCode() == null && webhook.getData().getOrderCode() != null) {
                payment.setPayosOrderCode(webhook.getData().getOrderCode());
            }
            payment.setPayosTransactionRef(webhook.getData().getReference());
            payment.setBankCode(webhook.getData().getCounterAccountBankId());
            payment.setBankName(webhook.getData().getCounterAccountBankName());
            payment.setAccountNumber(webhook.getData().getAccountNumber());
            payment.setCounterAccountName(webhook.getData().getCounterAccountName());
            payment.setCounterAccountNumber(webhook.getData().getCounterAccountNumber());
            payment.setTransactionDatetime(parseDateTime(webhook.getData().getTransactionDateTime()));

            commandRepository.save(payment);

            // 7. Save history
            saveHistory(payment.getOrderId(), oldStatus, newStatus,
                    "Webhook received - code: " + webhook.getData().getCode());

            // 8. Sync to MongoDB
            eventPublisher.publishEvent(new PaymentCreatedEvent(payment));

            // 9. Publish Kafka event if SUCCESS (only if status changed to SUCCESS)
            // This ensures we only publish once per successful payment
            if (newStatus == Status.SUCCESS && oldStatus != Status.SUCCESS) {
                PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                        .orderId(payment.getOrderId())
                        .subscriptionId(payment.getSubscriptionId())
                        .userId(payment.getUserId())
                        .planId(payment.getPlanId())
                        .amount(payment.getAmount())
                        .payosPaymentLinkId(payment.getPayosPaymentLinkId())
                        .payosTransactionRef(payment.getPayosTransactionRef())
                        .completedAt(LocalDateTime.now())
                        .build();

                paymentCompletedKafkaTemplate.send(paymentCompletedTopic, event);
                log.info("Published payment completed event to Kafka - orderId: {}, subscriptionId: {}",
                        payment.getOrderId(), payment.getSubscriptionId());
            } else if (newStatus == Status.SUCCESS && oldStatus == Status.SUCCESS) {
                log.warn("Payment {} already SUCCESS. Skipping Kafka event to avoid duplicate. Webhook ID: {}",
                        payment.getOrderId(), webhookId);
            }

            // 10. Mark webhook as processed
            webhookLog.setProcessed(true);
            webhookLog.setProcessedAt(LocalDateTime.now());
            webhookLogRepository.save(webhookLog);

            log.info("Webhook processed successfully - orderId: {}, status: {}",
                    payment.getOrderId(), newStatus);

        } catch (Exception e) {
            log.error("Failed to process webhook", e);
            throw new RuntimeException("Webhook processing failed", e);
        }
    }

    private void saveHistory(String orderId, Status fromStatus, Status toStatus, String reason) {
        PaymentHistory history = PaymentHistory.builder()
                .orderId(orderId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(reason)
                .build();
        historyRepository.save(history);
    }

    /**
     * Process payment from return URL params (fallback if webhook fails)
     * This is called when user is redirected back from PayOS after payment
     */
    public void processPaymentFromReturnUrl(String paymentLinkId, String code, String status) {
        log.info("Processing payment from return URL - paymentLinkId: {}, code: {}, status: {}",
                paymentLinkId, code, status);

        try {
            // Find payment by paymentLinkId
            PaymentCommand payment = commandRepository
                    .findByPayosPaymentLinkId(paymentLinkId)
                    .orElseThrow(() -> new RuntimeException("Payment not found for paymentLinkId: " + paymentLinkId));

            // Check if already SUCCESS
            if (payment.getStatus() == Status.SUCCESS) {
                log.info("Payment {} already SUCCESS. Skipping processing.", payment.getOrderId());
                return;
            }

            // Create a mock webhook request from return URL params
            PayOSWebhookRequest webhookRequest = new PayOSWebhookRequest();
            webhookRequest.setCode(code);
            webhookRequest.setDesc(status);
            webhookRequest.setSuccess("00".equals(code));

            PayOSWebhookData webhookData = new PayOSWebhookData();
            webhookData.setPaymentLinkId(paymentLinkId);
            webhookData.setCode(code);
            webhookData.setDesc(status);
            webhookRequest.setData(webhookData);

            // Generate a simple signature for the mock webhook (not verified, but needed
            // for processing)
            // In production, webhook should have proper signature from PayOS
            webhookRequest.setSignature("return-url-signature");

            // Process the webhook
            processWebhook(webhookRequest);

            log.info("Payment {} processed successfully from return URL", payment.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process payment from return URL - paymentLinkId: {}", paymentLinkId, e);
            throw new RuntimeException("Failed to process payment from return URL", e);
        }
    }

    private Status mapPayOSStatus(String code) {
        return switch (code) {
            case "00" -> Status.SUCCESS;
            case "01" -> Status.FAILED;
            case "02" -> Status.CANCELLED;
            default -> Status.FAILED;
        };
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null)
            return null;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", dateTimeStr);
            return null;
        }
    }

    private PaymentResponse convertToResponse(PaymentCommand payment) {
        return PaymentResponse.builder()
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .subscriptionId(payment.getSubscriptionId())
                .planId(payment.getPlanId())
                .amount(payment.getAmount())
                .orderInfo(payment.getOrderInfo())
                .payosPaymentLinkId(payment.getPayosPaymentLinkId())
                .payosTransactionRef(payment.getPayosTransactionRef())
                .paymentUrl(payment.getPaymentUrl())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}