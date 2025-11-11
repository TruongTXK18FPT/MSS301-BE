package com.mss301.paymentservice.service;

import com.mss301.paymentservice.model.dtos.request.PaymentRequest;
import com.mss301.paymentservice.model.dtos.response.PaymentResponse;

import java.util.Map;

public interface PaymentCommandService {
    PaymentResponse createPayment(PaymentRequest request);
    PaymentResponse processPaymentCallback(Map<String, String> params);
}
