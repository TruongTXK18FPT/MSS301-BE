package com.mss301.paymentservice.model.dtos.payos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PayOS Payment Data (inside response)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayOSPaymentData {

    @JsonProperty("bin")
    private String bin;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("paymentLinkId")
    private String paymentLinkId;

    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("description")
    private String description;

    @JsonProperty("orderCode")
    private Long orderCode;

    @JsonProperty("status")
    private String status; // PENDING, PAID, CANCELLED, EXPIRED

    @JsonProperty("checkoutUrl")
    private String checkoutUrl;

    @JsonProperty("qrCode")
    private String qrCode;
}
