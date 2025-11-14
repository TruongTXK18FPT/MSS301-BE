package com.mss301.paymentservice.model.dtos.payos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PayOS Payment Request DTO
 * Ref: https://payos.vn/docs/api/#tag/payment-request/operation/payment-request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayOSPaymentRequest {

    @JsonProperty("orderCode")
    private Long orderCode; // Unique order code (timestamp or ID)

    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("description")
    private String description;

    @JsonProperty("cancelUrl")
    private String cancelUrl;

    @JsonProperty("returnUrl")
    private String returnUrl;

    @JsonProperty("signature")
    private String signature; // HMAC_SHA256 signature

    @JsonProperty("items")
    private List<PayOSItem> items; // Optional

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayOSItem {
        private String name;
        private Integer quantity;
        private Long price;
    }
}
