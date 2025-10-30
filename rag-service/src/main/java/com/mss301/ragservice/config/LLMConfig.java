package com.mss301.ragservice.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import com.mss301.ragservice.enums.LLMProvider;
import com.mss301.ragservice.enums.ResponseMode;
import com.mss301.ragservice.service.llm.LLMService;
import com.mss301.ragservice.service.llm.impl.MistralLLMService;
import com.mss301.ragservice.service.llm.impl.GeminiLLMService;
import com.mss301.ragservice.strategy.ResponseStrategy;
import com.mss301.ragservice.strategy.impl.ChatResponseStrategy;
import com.mss301.ragservice.strategy.impl.MindmapResponseStrategy;
import com.mss301.ragservice.strategy.impl.ExerciseResponseStrategy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class LLMConfig {

    @Bean("mistralChatClient")
    @Primary
    public ChatClient mistralChatClient(ChatClient.Builder builder) {
        log.info("Creating Mistral ChatClient bean");
        return builder.build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Map<LLMProvider, LLMService> llmServices(
            MistralLLMService mistralService,
            GeminiLLMService geminiService) {
        log.info("Configuring LLM services map");

        Map<LLMProvider, LLMService> services = new HashMap<>();
        services.put(LLMProvider.MISTRAL, mistralService);
        services.put(LLMProvider.GEMINI, geminiService);

        log.info("Registered {} LLM provider(s): MISTRAL, GEMINI", services.size());
        return services;
    }

    @Bean
    public Map<ResponseMode, ResponseStrategy> responseStrategies(
            ChatResponseStrategy chatStrategy, 
            MindmapResponseStrategy mindmapStrategy,
            ExerciseResponseStrategy exerciseStrategy) {
        log.info("Configuring response strategies map");

        Map<ResponseMode, ResponseStrategy> strategies = new HashMap<>();
        strategies.put(ResponseMode.CHAT, chatStrategy);
        strategies.put(ResponseMode.MINDMAP, mindmapStrategy);
        strategies.put(ResponseMode.VOICECHAT, chatStrategy); // VOICECHAT reuses CHAT strategy
        strategies.put(ResponseMode.EXERCISE, exerciseStrategy);

        log.info("Registered {} response strategy(ies)", strategies.size());
        return strategies;
    }
}
