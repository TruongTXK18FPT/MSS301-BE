package com.mss301.paymentservice.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class MomoConfig {

    // Cấu hình MoMo
    @Value("${momo.access-key}")
    private String accessKey;

    @Value("${momo.secret-key}")
    private String secretKey;

    @Value("${momo.return-url}")
    private String returnUrl;

    @Value("${momo.payment-url}")
    private String paymentUrl;

    @Value("${momo.partner-code}")
    private String partnerCode;

    // URL này được sử dụng để chuyển trang (redirect) từ MoMo về trang mua hàng của đối tác sau khi khách hàng thanh toán.
    @Value("${momo.redirect-url}")
    private String redirectUrl;

    //URL này được sử dụng để chuyển trang (redirect) từ MoMo về trang mua hàng của đối tác sau khi khách hàng thanh toán.
    @Value("${momo.ipn-url}")
    private String ipnUrl;

    @Value("${momo.request-type}")
    private String requestType;

    private String extraData = "";

    // ngôn ngữ message trả về
    private String lang = "en";
}
