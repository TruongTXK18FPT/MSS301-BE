package com.mss301.paymentservice.service;

import com.mss301.paymentservice.model.PaymentQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Read operations for Payment domain (CQRS Query side)
 * Reads from MongoDB for optimized queries
 */
public interface PaymentReadService {

    /**
     * Find payment by order ID
     */
    Optional<PaymentQuery> findByOrderId(String orderId);

    /**
     * Find payments by user ID
     */
    List<PaymentQuery> findByUserId(Long userId);

    /**
     * Find payments by user ID with pagination
     */
    Page<PaymentQuery> findByUserId(Long userId, Pageable pageable);

    /**
     * Find all payments with pagination
     */
    Page<PaymentQuery> findAll(Pageable pageable);
}
