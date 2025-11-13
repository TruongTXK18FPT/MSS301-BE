package com.mss301.paymentservice.service;

import com.mss301.paymentservice.model.PaymentCommand;
import com.mss301.paymentservice.model.PaymentQuery;
import com.mss301.paymentservice.model.dtos.response.PaymentResponse;
import com.mss301.paymentservice.repository.PaymentCommandRepository;
import com.mss301.paymentservice.repository.PaymentQueryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class PaymentQueryServiceImp implements PaymentQueryService {

    @Autowired
    private PaymentQueryRepository queryRepository;

    @Autowired
    private PaymentCommandRepository commandRepository;

    @Override
    public List<PaymentResponse> findAllPayments() {
        log.info("Finding all payments from MongoDB");
        return queryRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentResponse findByOrderId(String orderId) {
        log.info("Finding payment by orderId: {}", orderId);

        // Try MongoDB first (Query model - read optimized)
        // Try by UUID orderId first
        PaymentQuery query = queryRepository.findByOrderId(orderId);
        if (query != null) {
            log.debug("Found payment in MongoDB by UUID: {}", orderId);
            return convertToResponse(query);
        }

        // Try by PayOS orderCode in MongoDB (if orderId is actually a PayOS orderCode)
        try {
            Long orderCode = Long.parseLong(orderId);
            log.debug("Trying to find payment in MongoDB by PayOS orderCode: {}", orderCode);
            query = queryRepository.findByPayosOrderCode(orderCode);
            if (query != null) {
                log.debug("Found payment in MongoDB by PayOS orderCode: {}", orderCode);
                return convertToResponse(query);
            }
        } catch (NumberFormatException e) {
            // orderId is not a number, skip PayOS orderCode lookup
            log.debug("orderId is not a number, skipping PayOS orderCode lookup in MongoDB");
        }

        // Fallback to PostgreSQL (Command model - source of truth)
        // Try by UUID orderId first
        log.debug("Payment not found in MongoDB, checking PostgreSQL by UUID: {}", orderId);
        Optional<PaymentCommand> command = commandRepository.findById(orderId);
        if (command.isPresent()) {
            log.debug("Found payment in PostgreSQL by UUID: {}", orderId);
            return convertFromCommand(command.get());
        }

        // Try by PayOS orderCode in PostgreSQL (if orderId is actually a PayOS
        // orderCode)
        try {
            Long orderCode = Long.parseLong(orderId);
            log.debug("Trying to find payment in PostgreSQL by PayOS orderCode: {}", orderCode);
            command = commandRepository.findByPayosOrderCode(orderCode);
            if (command.isPresent()) {
                log.debug("Found payment in PostgreSQL by PayOS orderCode: {}", orderCode);
                return convertFromCommand(command.get());
            }
        } catch (NumberFormatException e) {
            // orderId is not a number, skip PayOS orderCode lookup
            log.debug("orderId is not a number, skipping PayOS orderCode lookup in PostgreSQL");
        }

        // Not found in both
        log.warn("Payment not found in both MongoDB and PostgreSQL: {}", orderId);
        throw new RuntimeException("Payment not found: " + orderId);
    }

    @Override
    public Page<PaymentResponse> findByUserId(Long userId, Pageable pageable) {
        log.info("Finding payments by user ID: {} with pagination", userId);
        Page<PaymentQuery> payments = queryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return payments.map(this::convertToResponse);
    }

    private PaymentResponse convertToResponse(PaymentQuery payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null");
        }

        return PaymentResponse.builder()
                .orderId(payment.getOrderId())
                .subscriptionId(payment.getSubscriptionId())
                .planId(payment.getPlanId())
                .userId(payment.getUserId())
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

    /**
     * Convert PaymentCommand (PostgreSQL) to PaymentResponse
     * Used as fallback when payment is not yet synced to MongoDB
     */
    private PaymentResponse convertFromCommand(PaymentCommand payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null");
        }

        return PaymentResponse.builder()
                .orderId(payment.getOrderId())
                .subscriptionId(payment.getSubscriptionId())
                .planId(payment.getPlanId())
                .userId(payment.getUserId())
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
