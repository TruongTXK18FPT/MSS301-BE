package com.mss301.paymentservice.service;

import com.mss301.paymentservice.model.dtos.request.PaymentRequest;
import com.mss301.paymentservice.model.dtos.response.PaymentResponse;

import java.util.Map;

/**
 * Write operations for Payment domain (CQRS Command side)
 * Handles payment creation, updates, and callback processing
 */
public interface PaymentWriteService {

    /**
     * Create a new payment and generate PayOS checkout URL
     */
    PaymentResponse createPayment(PaymentRequest request);

    /**
     * Process PayOS payment callback (webhook)
     * 
     * @param params Callback parameters from PayOS
     */
    PaymentResponse processPaymentCallback(Map<String, String> params);
}
