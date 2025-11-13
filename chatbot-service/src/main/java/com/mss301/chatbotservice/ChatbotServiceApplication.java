package com.mss301.chatbotservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {
    org.springframework.ai.model.mistralai.autoconfigure.MistralAiChatAutoConfiguration.class,
    org.springframework.ai.model.mistralai.autoconfigure.MistralAiEmbeddingAutoConfiguration.class,
    org.springframework.ai.model.mistralai.autoconfigure.MistralAiModerationAutoConfiguration.class
})
@EnableFeignClients
@EnableDiscoveryClient
@EnableJpaRepositories
public class ChatbotServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatbotServiceApplication.class, args);
    }
}
