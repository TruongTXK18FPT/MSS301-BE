package com.mss301.paymentservice.controller;

import com.mss301.paymentservice.model.dtos.request.PaymentRequest;
import com.mss301.paymentservice.model.dtos.response.ApiResponse;
import com.mss301.paymentservice.model.dtos.response.PaymentResponse;
import com.mss301.paymentservice.service.PaymentCommandService;
import com.mss301.paymentservice.service.PaymentCommandServiceImp;
import com.mss301.paymentservice.service.PaymentQueryService;
import com.mss301.paymentservice.service.PaymentQueryServiceImp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payment")
@CrossOrigin
@Slf4j
public class PaymentController {

    @Autowired
    private PaymentCommandService commandService;

    @Autowired
    private PaymentQueryService queryService;

    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(
            @RequestBody PaymentRequest request) {
        try {
            PaymentResponse response = commandService.createPayment(request);

            return ApiResponse.<PaymentResponse>builder()
                    .code(200)
                    .message("Payment created successfully")
                    .result(response)
                    .build();
        } catch (Exception e) {
            log.error("Error creating payment", e);
            return ApiResponse.<PaymentResponse>builder()
                            .code(500)
                            .message("Failed to create payment: " + e.getMessage())
                            .build();
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> findAllPayments() {
        try {
            List<PaymentResponse> payments = queryService.findAllPayments();
            return ResponseEntity.ok(ApiResponse.<List<PaymentResponse>>builder()
                    .code(200)
                    .message("Payments retrieved successfully")
                    .result(payments)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving all payments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<PaymentResponse>>builder()
                            .code(500)
                            .message("Failed to retrieve payments")
                            .build());
        }
    }

    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> findByOrderId(
            @PathVariable("subscriptionId") Long subscriptionId) {

        try {
            PaymentResponse payment = queryService.findBySubscriptionId(subscriptionId);
            if (payment == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<PaymentResponse>builder()
                                .code(404)
                                .message("Payment not found")
                                .build());
            }

            return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                    .code(200)
                    .message("Payment found")
                    .result(payment)
                    .build());
        } catch (Exception e) {
            log.error("Error finding payment by subscription ID: {}", subscriptionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<PaymentResponse>builder()
                            .code(500)
                            .message("Failed to find payment")
                            .build());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> findByUserId(
            @PathVariable("userId") Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        try {
            Page<PaymentResponse> payments = queryService.findByUserId(userId, pageable);
            return ResponseEntity.ok(ApiResponse.<Page<PaymentResponse>>builder()
                    .code(200)
                    .message("User payments retrieved successfully")
                    .result(payments)
                    .build());
        } catch (Exception e) {
            log.error("Error finding payments by user ID: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Page<PaymentResponse>>builder()
                            .code(500)
                            .message("Failed to retrieve user payments")
                            .build());
        }
    }

    @PostMapping("/momo/callback")
    public ResponseEntity<String> handleMomoCallback(@RequestParam Map<String, String> params) {
        try {
            commandService.processPaymentCallback(params);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing MoMo callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR");
        }
    }

    @PostMapping("/momo/ipn")
    public ResponseEntity<String> handleMomoIPN(@RequestParam Map<String, String> params) {
        try {
            commandService.processPaymentCallback(params);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing MoMo IPN", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ERROR");
        }
    }
}
