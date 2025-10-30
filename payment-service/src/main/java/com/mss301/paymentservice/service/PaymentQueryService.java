package com.mss301.paymentservice.service;

import com.mss301.paymentservice.model.dtos.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentQueryService {
    List<PaymentResponse> findAllPayments();

    PaymentResponse findBySubscriptionId(Long subscriptionId);

    Page<PaymentResponse> findByUserId(Long userId, Pageable pageable);
}
