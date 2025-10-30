package com.mss301.paymentservice.util;

import com.mss301.paymentservice.config.MomoConfig;
import com.mss301.paymentservice.model.dtos.request.MomoRequest;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class MomoUtil {
    public static Map<String, Object> createRequestMap(MomoConfig config, MomoRequest request) {
        String rawData = String.format(
                "accessKey=%s&amount=%d&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                config.getAccessKey(),
                request.getAmount(),
                config.getExtraData(),
                config.getIpnUrl(),
                request.getOrderId(),
                request.getOrderInfo(),
                config.getPartnerCode(),
                config.getRedirectUrl(),
                request.getRequestId(),
                config.getRequestType()
        );

        String signature = hmacSHA256(rawData, config.getSecretKey());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("partnerCode", config.getPartnerCode());
        requestBody.put("accessKey", config.getAccessKey());
        requestBody.put("requestId", request.getRequestId());
        requestBody.put("amount", request.getAmount());
        requestBody.put("orderId", request.getOrderId());
        requestBody.put("orderInfo", request.getOrderInfo());
        requestBody.put("redirectUrl", config.getRedirectUrl());
        requestBody.put("ipnUrl", config.getIpnUrl());
        requestBody.put("extraData", config.getExtraData());
        requestBody.put("requestType", config.getRequestType());
        requestBody.put("signature", signature);
        requestBody.put("lang", config.getLang());

        return requestBody;
    }

    private static String hmacSHA256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes());
            return Hex.encodeHexString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC signature", e);
        }
    }
}
