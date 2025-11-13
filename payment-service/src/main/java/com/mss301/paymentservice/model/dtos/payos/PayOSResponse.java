package com.mss301.paymentservice.model.dtos.payos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PayOS API Response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayOSResponse<T> {

    @JsonProperty("code")
    private String code; // "00" = success

    @JsonProperty("desc")
    private String desc;

    @JsonProperty("data")
    private T data;

    @JsonProperty("signature")
    private String signature;

    public boolean isSuccess() {
        return "00".equals(code);
    }
}
