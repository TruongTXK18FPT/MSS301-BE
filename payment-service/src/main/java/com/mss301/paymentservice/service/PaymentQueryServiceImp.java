package com.mss301.paymentservice.service;

import com.mss301.paymentservice.model.PaymentQuery;
import com.mss301.paymentservice.model.dtos.response.PaymentResponse;
import com.mss301.paymentservice.repository.PaymentQueryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class PaymentQueryServiceImp implements PaymentQueryService {

    @Autowired
    private PaymentQueryRepository queryRepository;

    @Override
    public List<PaymentResponse> findAllPayments() {
        log.info("Finding all payments from MongoDB");
        return queryRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentResponse findBySubscriptionId(Long subscriptionId) {
        log.info("Finding payment by subscription ID: {}", subscriptionId);
        PaymentQuery payment = queryRepository.findBySubscriptionId(subscriptionId);
        return payment != null ? convertToResponse(payment) : null;
    }

    @Override
    public Page<PaymentResponse> findByUserId(Long userId, Pageable pageable) {
        log.info("Finding payments by user ID: {} with pagination", userId);
        Page<PaymentQuery> payments = queryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return payments.map(this::convertToResponse);
    }

    private PaymentResponse convertToResponse(PaymentQuery payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .subscriptionId(payment.getSubscriptionId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .orderInfo(payment.getOrderInfo())
                .paymentUrl(payment.getPaymentUrl())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
