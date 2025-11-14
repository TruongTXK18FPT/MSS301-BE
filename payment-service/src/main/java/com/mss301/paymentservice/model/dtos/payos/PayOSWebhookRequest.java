package com.mss301.paymentservice.model.dtos.payos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PayOS Webhook Request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayOSWebhookRequest {

    @JsonProperty("code")
    private String code;

    @JsonProperty("desc")
    private String desc;

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("data")
    private PayOSWebhookData data;

    @JsonProperty("signature")
    private String signature;
}
