package com.mss301.premiumservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PremiumServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PremiumServiceApplication.class, args);
    }
}
