package com.mss301.chatbotservice.chatbot.model;

import lombok.Data;

import java.util.List;

@Data
public class AiResponse {
    private String id;
    private String provider;
    private String model;
    private String object;
    private long created;
    private List<Choice> choices;
    private Usage usage;
}
