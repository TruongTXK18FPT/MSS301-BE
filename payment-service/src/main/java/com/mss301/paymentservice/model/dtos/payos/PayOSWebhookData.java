package com.mss301.paymentservice.model.dtos.payos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PayOS Webhook Payload
 * Ref: https://payos.vn/docs/du-lieu-tra-ve/webhook/
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayOSWebhookData {

    @JsonProperty("orderCode")
    private Long orderCode;

    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("description")
    private String description;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("reference")
    private String reference; // Transaction reference from bank

    @JsonProperty("transactionDateTime")
    private String transactionDateTime;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("paymentLinkId")
    private String paymentLinkId;

    @JsonProperty("code")
    private String code; // "00" = success

    @JsonProperty("desc")
    private String desc;

    @JsonProperty("counterAccountBankId")
    private String counterAccountBankId;

    @JsonProperty("counterAccountBankName")
    private String counterAccountBankName;

    @JsonProperty("counterAccountName")
    private String counterAccountName;

    @JsonProperty("counterAccountNumber")
    private String counterAccountNumber;

    @JsonProperty("virtualAccountName")
    private String virtualAccountName;

    @JsonProperty("virtualAccountNumber")
    private String virtualAccountNumber;
}
