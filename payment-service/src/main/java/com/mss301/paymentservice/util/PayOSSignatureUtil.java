package com.mss301.paymentservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * PayOS Signature Utility
 * Implements HMAC-SHA256 signature verification as per PayOS docs
 * Ref: https://payos.vn/docs/tich-hop-webhook/kiem-tra-du-lieu-voi-signature/
 */
@Component
@Slf4j
public class PayOSSignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generate signature for payment request
     * Data format:
     * amount={amount}&cancelUrl={cancelUrl}&description={description}&orderCode={orderCode}&returnUrl={returnUrl}
     */
    public String generatePaymentSignature(
            Long amount,
            String cancelUrl,
            String description,
            Long orderCode,
            String returnUrl,
            String checksumKey) {
        try {
            // Sort parameters alphabetically
            TreeMap<String, String> params = new TreeMap<>();
            params.put("amount", String.valueOf(amount));
            params.put("cancelUrl", cancelUrl);
            params.put("description", description);
            params.put("orderCode", String.valueOf(orderCode));
            params.put("returnUrl", returnUrl);

            // Build data string
            String data = params.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("&"));

            return generateHmacSHA256(data, checksumKey);
        } catch (Exception e) {
            log.error("Error generating payment signature", e);
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    /**
     * Verify webhook signature
     * The signature is HMAC_SHA256 of the sorted webhook data
     */
    public boolean verifyWebhookSignature(
            String receivedSignature,
            Object webhookData,
            String checksumKey) {
        try {
            // Convert data to JSON string (PayOS sends sorted JSON)
            String dataString = objectMapper.writeValueAsString(webhookData);

            // Generate expected signature
            String expectedSignature = generateHmacSHA256(dataString, checksumKey);

            boolean isValid = expectedSignature.equalsIgnoreCase(receivedSignature);

            if (!isValid) {
                log.warn("Webhook signature mismatch. Expected: {}, Received: {}",
                        expectedSignature, receivedSignature);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }

    /**
     * Generate HMAC-SHA256 hash
     */
    private String generateHmacSHA256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256);
        mac.init(secretKeySpec);

        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * Generate unique webhook ID for idempotency check
     */
    public String generateWebhookId(String paymentLinkId, String signature) {
        return paymentLinkId + "_" + signature.substring(0, Math.min(16, signature.length()));
    }
}
