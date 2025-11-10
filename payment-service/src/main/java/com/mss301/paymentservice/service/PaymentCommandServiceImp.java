package com.mss301.paymentservice.service;

import com.mss301.paymentservice.config.MomoConfig;
import com.mss301.paymentservice.constant.Status;
import com.mss301.paymentservice.event.PaymentCreatedEvent;
import com.mss301.paymentservice.event.PaymentStatusUpdatedEvent;
import com.mss301.paymentservice.model.PaymentCommand;
import com.mss301.paymentservice.model.dtos.request.MomoRequest;
import com.mss301.paymentservice.model.dtos.request.PaymentRequest;
import com.mss301.paymentservice.model.dtos.response.*;
import com.mss301.paymentservice.repository.PaymentCommandRepository;
import com.mss301.paymentservice.util.MomoUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class PaymentCommandServiceImp implements PaymentCommandService {

    @Autowired
    private PaymentCommandRepository commandRepository;

    @Autowired
    private MomoConfig momoConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserService userService;

    @Override
    public PaymentResponse createPayment(PaymentRequest request, Long userId) {
        log.info("Creating payment for user: {}, subscription: {}", userId, request.getSubscriptionId());

        PaymentCommand payment = createPaymentCommand(request, userId);
        PaymentCommand saved = commandRepository.save(payment);

        // Create MoMo payment URL
        try {
            String paymentUrl = createMomoPaymentUrl(saved);
            saved.setPaymentUrl(paymentUrl);
            saved = commandRepository.save(saved);

            log.info("Payment created successfully with ID: {}", saved.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to create MoMo payment URL for payment: {}", saved.getPaymentId(), e);
            saved.setStatus(Status.FAILED);
            saved = commandRepository.save(saved);
        }

        // Publish event for MongoDB sync
        eventPublisher.publishEvent(new PaymentCreatedEvent(saved));

        return convertToResponse(saved);
    }

    @Override
    public PaymentResponse processPaymentCallback(Map<String, String> params) {
        String subscriptionId = params.get("orderId");
        String resultCode = params.get("resultCode");

        PaymentCommand payment = commandRepository.findBySubscriptionId(Long.parseLong(subscriptionId));
        if (payment == null) {
            throw new EntityNotFoundException("Payment not found for subscriptionId: " + subscriptionId);
        }

        Status newStatus = "0".equals(resultCode) ? Status.SUCCESS : Status.FAILED;
        payment.setStatus(newStatus);
        payment.setMomoTransId(params.get("transId"));
        payment.setUpdatedAt(LocalDateTime.now());

        PaymentCommand updated = commandRepository.save(payment);

        // Publish event for MongoDB sync
        eventPublisher.publishEvent(new PaymentStatusUpdatedEvent(updated));

        log.info("Payment callback processed. Status: {}", newStatus);

        return convertToResponse(updated);
    }

    private PaymentCommand createPaymentCommand(PaymentRequest request, Long userId) {
        PaymentCommand payment = new PaymentCommand();
        payment.setSubscriptionId(request.getSubscriptionId());
        payment.setUserId(userId);
        payment.setAmount(getAmount(payment.getSubscriptionId()));
        payment.setOrderInfo(request.getOrderInfo());
        payment.setStatus(Status.PENDING);
        return payment;
    }

    private String createMomoPaymentUrl(PaymentCommand payment) {
        MomoRequest momoRequest = new MomoRequest();
        momoRequest.setRequestId(UUID.randomUUID().toString());
        momoRequest.setAmount(payment.getAmount());
        momoRequest.setSubscriptionId(payment.getSubscriptionId());
        momoRequest.setOrderInfo(payment.getOrderInfo());

        payment.setMomoRequestId(momoRequest.getRequestId());

        Map<String, Object> requestBody = MomoUtil.createRequestMap(momoConfig, momoRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(
                    momoConfig.getPaymentUrl(), httpEntity, Map.class);

            if (response == null || !"0".equals(String.valueOf(response.get("resultCode")))) {
                String errorMsg = response != null ? (String) response.get("message") : "Unknown error";
                throw new RuntimeException("Failed to create MoMo payment URL: " + errorMsg);
            }

            return (String) response.get("payUrl");
        } catch (Exception e) {
            log.error("Error calling MoMo API", e);
            throw new RuntimeException("Failed to create MoMo payment URL", e);
        }
    }

    private Long getAmount(Long subscriptionId) {
        ResponseEntity<SubscriptionResponse> subscription = subscriptionService.findBySubscriptionId(subscriptionId);
        Long planId = subscription.getBody().getPlanId();
        ResponseEntity<PlanResponse> plan = subscriptionService.findByPlanId(planId);
        return plan.getBody().getPriceCents();
    }

    private PaymentResponse convertToResponse(PaymentCommand payment) {
        ResponseEntity<SubscriptionResponse> subscriptionResponse = subscriptionService.findBySubscriptionId(payment.getSubscriptionId());
        ApiResponse<UserResponse> userResponse = userService.getUserById(payment.getUserId());

        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .subscription(subscriptionResponse.getBody())
                .user(userResponse.getResult())
                .amount(payment.getAmount())
                .orderInfo(payment.getOrderInfo())
                .paymentUrl(payment.getPaymentUrl())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
