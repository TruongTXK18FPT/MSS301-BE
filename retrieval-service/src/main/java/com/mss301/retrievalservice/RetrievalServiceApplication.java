package com.mss301.retrievalservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class RetrievalServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(RetrievalServiceApplication.class, args);
	}

}
