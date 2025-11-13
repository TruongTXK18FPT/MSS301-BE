package com.mss301.paymentservice.service;

import com.mss301.paymentservice.config.MomoConfig;
import com.mss301.paymentservice.constant.Status;
import com.mss301.paymentservice.event.PaymentCompletedEvent;
import com.mss301.paymentservice.event.PaymentCreatedEvent;
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

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("Creating payment for user: {}, subscription: {}",
                request.getUserId(), request.getPlanId());

        // ✅ Validate request
        validatePaymentRequest(request);

        PaymentCommand payment = createPaymentCommand(request);
        PaymentCommand saved = commandRepository.save(payment);

        try {
            String paymentUrl = createMomoPaymentUrl(saved);
            saved.setPaymentUrl(paymentUrl);
            saved = commandRepository.save(saved);

            log.info("Payment created successfully with ID: {}", saved.getOrderId());
        } catch (Exception e) {
            log.error("Failed to create MoMo payment URL for payment: {}",
                    saved.getOrderId(), e);
            saved.setStatus(Status.FAILED);
            saved = commandRepository.save(saved);
        }

        // Publish event for MongoDB sync
        eventPublisher.publishEvent(new PaymentCreatedEvent(saved));

        return convertToResponse(saved);
    }

    @Override
    public PaymentResponse processPaymentCallback(Map<String, String> params) {
        String orderId = params.get("orderId");
        String resultCode = params.get("resultCode");

        // ✅ Find by paymentId instead of subscriptionId
        PaymentCommand payment = commandRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Payment not found for orderId: " + orderId));

        Status newStatus = "0".equals(resultCode) ? Status.SUCCESS : Status.FAILED;
        payment.setStatus(newStatus);
        payment.setMomoTransId(params.get("transId"));
        payment.setUpdatedAt(LocalDateTime.now());

        PaymentCommand updated = commandRepository.save(payment);

        // ✅ Publish event để Premium Service xử lý
        if (newStatus == Status.SUCCESS) {
            eventPublisher.publishEvent(new PaymentCompletedEvent(
                    this,
                    updated.getOrderId(),
                    updated.getUserId(),
                    updated.getPlanId(),
                    updated.getAmount(),
                    updated.getStatus(),
                    updated.getMomoTransId(),
                    updated.getUpdatedAt()
            ));
        }

        log.info("Payment callback processed. Payment ID: {}, Status: {}",
                updated.getOrderId(), newStatus);

        return convertToResponse(updated);
    }

    private void validatePaymentRequest(PaymentRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (request.getPlanId() == null) {
            throw new IllegalArgumentException("Subscription ID is required");
        }
        if (request.getPlanId() == null) {
            throw new IllegalArgumentException("Plan ID is required");
        }
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
    }

    private PaymentCommand createPaymentCommand(PaymentRequest request) {
        PaymentCommand payment = new PaymentCommand();
        payment.setPlanId(request.getPlanId());
        payment.setUserId(request.getUserId());
        payment.setAmount(request.getAmount()); // ✅ Lấy từ request
        payment.setPlanId(request.getPlanId()); // ✅ Lưu planId
        payment.setOrderInfo(request.getOrderInfo());
        payment.setStatus(Status.PENDING);
        return payment;
    }

    private String createMomoPaymentUrl(PaymentCommand payment) {
        MomoRequest momoRequest = new MomoRequest();
        momoRequest.setRequestId(UUID.randomUUID().toString());
        momoRequest.setAmount(payment.getAmount());
        momoRequest.setPlanId(payment.getPlanId());
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

    private PaymentResponse convertToResponse(PaymentCommand payment) {
        return PaymentResponse.builder()
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .planId(payment.getPlanId())
                .amount(payment.getAmount())
                .orderInfo(payment.getOrderInfo())
                .paymentUrl(payment.getPaymentUrl())
                .status(payment.getStatus())
                .momoTransId(payment.getMomoTransId())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
