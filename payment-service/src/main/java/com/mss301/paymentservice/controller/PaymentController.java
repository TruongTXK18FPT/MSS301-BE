package com.mss301.paymentservice.controller;

import com.mss301.paymentservice.model.dtos.request.PaymentRequest;
import com.mss301.paymentservice.model.dtos.payos.PayOSWebhookRequest;
import com.mss301.paymentservice.model.dtos.response.ApiResponse;
import com.mss301.paymentservice.model.dtos.response.PaymentResponse;
import com.mss301.paymentservice.service.PaymentCommandService;
import com.mss301.paymentservice.service.PaymentQueryService;
import com.mss301.paymentservice.service.impl.PayOSPaymentServiceImpl;
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

@RestController
@RequestMapping("/payment")
@Slf4j
public class PaymentController {

    @Autowired
    private PaymentCommandService commandService;

    @Autowired
    private PaymentQueryService queryService;

    @Autowired
    private PayOSPaymentServiceImpl payOSPaymentService;

    /**
     * Create payment with PayOS
     */
    @PostMapping("/create")
    public ApiResponse<PaymentResponse> createPayment(
            @RequestBody PaymentRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            // Set userId from JWT token
            request.setUserId(userId);

            PaymentResponse response = payOSPaymentService.createPayment(request);

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

    /**
     * PayOS webhook callback (no auth required - whitelisted in gateway)
     */
    @PostMapping("/callback/payos")
    public ResponseEntity<String> handlePayOSWebhook(@RequestBody PayOSWebhookRequest webhook) {
        try {
            log.info("Received PayOS webhook - paymentLinkId: {}, code: {}",
                    webhook.getData().getPaymentLinkId(), webhook.getData().getCode());
            payOSPaymentService.processWebhook(webhook);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing PayOS webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Webhook processing failed");
        }
    }

    /**
     * Process payment from return URL params (fallback if webhook fails)
     * Called when user is redirected back from PayOS
     * Params: orderCode, code, id (paymentLinkId), cancel, status
     */
    @GetMapping("/callback/payos/return")
    public ResponseEntity<String> handlePayOSReturn(
            @RequestParam(required = false) String orderCode,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String id, // paymentLinkId
            @RequestParam(required = false) String cancel,
            @RequestParam(required = false) String status) {
        try {
            log.info("Received PayOS return callback - orderCode: {}, code: {}, id: {}, cancel: {}, status: {}",
                    orderCode, code, id, cancel, status);

            // If cancel=true, payment was cancelled
            if ("true".equalsIgnoreCase(cancel) || "CANCELLED".equalsIgnoreCase(status)) {
                log.info("Payment cancelled by user - orderCode: {}", orderCode);
                return ResponseEntity.ok("Payment cancelled");
            }

            // If status=PAID and code=00, payment was successful
            // We need to manually trigger webhook processing
            if ("PAID".equalsIgnoreCase(status) && "00".equals(code) && id != null) {
                log.info("Payment successful from return URL - triggering webhook processing for paymentLinkId: {}",
                        id);

                // Find payment by paymentLinkId
                payOSPaymentService.processPaymentFromReturnUrl(id, code, status);

                return ResponseEntity.ok("Payment processed successfully");
            }

            log.warn("Unknown return URL params - orderCode: {}, code: {}, status: {}", orderCode, code, status);
            return ResponseEntity.ok("Payment status unknown");
        } catch (Exception e) {
            log.error("Error processing PayOS return callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Return callback processing failed");
        }
    }

    @PostMapping
    public ApiResponse<PaymentResponse> createPaymentLegacy(
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

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable String orderId) {
        try {
            PaymentResponse payment = queryService.findByOrderId(orderId);
            return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                    .code(200)
                    .message("Payment retrieved successfully")
                    .result(payment)
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving payment: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<PaymentResponse>builder()
                            .code(404)
                            .message("Payment not found")
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
}
