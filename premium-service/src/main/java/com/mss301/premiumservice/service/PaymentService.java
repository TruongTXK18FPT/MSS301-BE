package com.mss301.premiumservice.service;


import com.mss301.premiumservice.model.dtos.request.PaymentRequest;
import com.mss301.premiumservice.model.dtos.response.ApiResponse;
import com.mss301.premiumservice.model.dtos.response.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "${service.payment.name}", url = "${service.payment.url}")
public interface PaymentService {

    @PostMapping("/payment")
    ApiResponse<PaymentResponse> createPayment(@RequestBody PaymentRequest request);
}
