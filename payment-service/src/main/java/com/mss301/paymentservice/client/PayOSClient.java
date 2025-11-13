package com.mss301.paymentservice.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mss301.paymentservice.config.PayOSConfig;
import com.mss301.paymentservice.model.dtos.payos.*;
import com.mss301.paymentservice.util.PayOSSignatureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

/**
 * PayOS API Client
 * Handles communication with PayOS payment gateway
 */
@Component
@Slf4j
public class PayOSClient {

    private final PayOSConfig payOSConfig;
    private final PayOSSignatureUtil signatureUtil;
    private final ObjectMapper objectMapper;

    public PayOSClient(PayOSConfig payOSConfig, PayOSSignatureUtil signatureUtil) {
        this.payOSConfig = payOSConfig;
        this.signatureUtil = signatureUtil;
        this.objectMapper = new ObjectMapper();
        // Configure ObjectMapper to ignore unknown properties
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private WebClient getWebClient() {
        return WebClient.builder()
                .baseUrl(payOSConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-client-id", payOSConfig.getClientId())
                .defaultHeader("x-api-key", payOSConfig.getApiKey())
                .build();
    }

    /**
     * Create payment link
     * POST /v2/payment-requests
     */
    public PayOSResponse<PayOSPaymentData> createPaymentLink(
            Long orderCode,
            Long amount,
            String description,
            String returnUrl,
            String cancelUrl) {
        try {
            // Generate signature
            String signature = signatureUtil.generatePaymentSignature(
                    amount, cancelUrl, description, orderCode, returnUrl,
                    payOSConfig.getChecksumKey());

            // Build request
            PayOSPaymentRequest request = PayOSPaymentRequest.builder()
                    .orderCode(orderCode)
                    .amount(amount)
                    .description(description)
                    .cancelUrl(cancelUrl)
                    .returnUrl(returnUrl)
                    .signature(signature)
                    .items(Collections.singletonList(
                            PayOSPaymentRequest.PayOSItem.builder()
                                    .name(description)
                                    .quantity(1)
                                    .price(amount)
                                    .build()))
                    .build();

            log.info("Creating PayOS payment link for orderCode: {}, amount: {}", orderCode, amount);

            // Call PayOS API - deserialize as Map first, then convert to
            // PayOSResponse<PayOSPaymentData>
            Mono<Map<String, Object>> responseMono = getWebClient()
                    .post()
                    .uri("/v2/payment-requests")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> responseMap = responseMono.block();

            if (responseMap == null) {
                throw new RuntimeException("PayOS API returned null response");
            }

            // Convert Map to PayOSResponse<PayOSPaymentData>
            PayOSResponse<PayOSPaymentData> response = new PayOSResponse<>();
            response.setCode((String) responseMap.get("code"));
            response.setDesc((String) responseMap.get("desc"));
            response.setSignature((String) responseMap.get("signature"));

            // Convert data Map to PayOSPaymentData
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
            if (dataMap != null) {
                PayOSPaymentData paymentData = objectMapper.convertValue(dataMap, PayOSPaymentData.class);
                response.setData(paymentData);
            }

            if (response.isSuccess()) {
                log.info("PayOS payment link created successfully: {}", response.getData());
                return response;
            } else {
                log.error("PayOS payment creation failed: {}", response);
                throw new RuntimeException("Failed to create PayOS payment: " + response.getDesc());
            }

        } catch (Exception e) {
            log.error("Error calling PayOS API", e);
            throw new RuntimeException("PayOS API error: " + e.getMessage(), e);
        }
    }

    /**
     * Get payment info
     * GET /v2/payment-requests/{id}
     */
    public PayOSResponse<PayOSPaymentData> getPaymentInfo(String orderCodeOrPaymentLinkId) {
        try {
            log.info("Getting PayOS payment info for: {}", orderCodeOrPaymentLinkId);

            Mono<Map<String, Object>> responseMono = getWebClient()
                    .get()
                    .uri("/v2/payment-requests/" + orderCodeOrPaymentLinkId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> responseMap = responseMono.block();

            if (responseMap == null) {
                throw new RuntimeException("PayOS API returned null response");
            }

            // Convert Map to PayOSResponse<PayOSPaymentData>
            PayOSResponse<PayOSPaymentData> response = new PayOSResponse<>();
            response.setCode((String) responseMap.get("code"));
            response.setDesc((String) responseMap.get("desc"));
            response.setSignature((String) responseMap.get("signature"));

            // Convert data Map to PayOSPaymentData
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
            if (dataMap != null) {
                PayOSPaymentData paymentData = objectMapper.convertValue(dataMap, PayOSPaymentData.class);
                response.setData(paymentData);
            }

            return response;

        } catch (Exception e) {
            log.error("Error getting PayOS payment info", e);
            throw new RuntimeException("Failed to get payment info: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel payment
     * POST /v2/payment-requests/{id}/cancel
     */
    public PayOSResponse<PayOSPaymentData> cancelPayment(String orderCodeOrPaymentLinkId, String reason) {
        try {
            log.info("Cancelling PayOS payment: {}", orderCodeOrPaymentLinkId);

            Mono<Map<String, Object>> responseMono = getWebClient()
                    .post()
                    .uri("/v2/payment-requests/" + orderCodeOrPaymentLinkId + "/cancel")
                    .bodyValue(Collections.singletonMap("cancellationReason", reason))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> responseMap = responseMono.block();

            if (responseMap == null) {
                throw new RuntimeException("PayOS API returned null response");
            }

            // Convert Map to PayOSResponse<PayOSPaymentData>
            PayOSResponse<PayOSPaymentData> response = new PayOSResponse<>();
            response.setCode((String) responseMap.get("code"));
            response.setDesc((String) responseMap.get("desc"));
            response.setSignature((String) responseMap.get("signature"));

            // Convert data Map to PayOSPaymentData
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
            if (dataMap != null) {
                PayOSPaymentData paymentData = objectMapper.convertValue(dataMap, PayOSPaymentData.class);
                response.setData(paymentData);
            }

            return response;

        } catch (Exception e) {
            log.error("Error cancelling PayOS payment", e);
            throw new RuntimeException("Failed to cancel payment: " + e.getMessage(), e);
        }
    }
}
