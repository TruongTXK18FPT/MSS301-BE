package com.mss301.documentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableFeignClients
public class DocumentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentServiceApplication.class, args);
    }
}
