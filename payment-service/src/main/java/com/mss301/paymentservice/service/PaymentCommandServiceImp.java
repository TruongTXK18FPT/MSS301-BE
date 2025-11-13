package com.mss301.paymentservice.service;

import com.mss301.paymentservice.constant.Status;
import com.mss301.paymentservice.event.PaymentCompletedEvent;
import com.mss301.paymentservice.event.PaymentCreatedEvent;
import com.mss301.paymentservice.model.PaymentCommand;
import com.mss301.paymentservice.model.dtos.request.PaymentRequest;
import com.mss301.paymentservice.model.dtos.response.*;
import com.mss301.paymentservice.repository.PaymentCommandRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Transactional
@Slf4j
public class PaymentCommandServiceImp implements PaymentCommandService {

    @Autowired
    private PaymentCommandRepository commandRepository;

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

        log.info("Payment created successfully with ID: {}", saved.getOrderId());

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
        payment.setUpdatedAt(LocalDateTime.now());

        PaymentCommand updated = commandRepository.save(payment);

        // ✅ Publish event để Premium Service xử lý
        if (newStatus == Status.SUCCESS) {
            eventPublisher.publishEvent(PaymentCompletedEvent.builder()
                    .orderId(updated.getOrderId())
                    .userId(updated.getUserId())
                    .subscriptionId(updated.getSubscriptionId())
                    .planId(updated.getPlanId())
                    .amount(updated.getAmount())
                    .payosPaymentLinkId(updated.getPayosPaymentLinkId())
                    .payosTransactionRef(updated.getPayosTransactionRef())
                    .completedAt(updated.getUpdatedAt())
                    .build());
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

    private PaymentResponse convertToResponse(PaymentCommand payment) {
        return PaymentResponse.builder()
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .planId(payment.getPlanId())
                .amount(payment.getAmount())
                .orderInfo(payment.getOrderInfo())
                .paymentUrl(payment.getPaymentUrl())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
