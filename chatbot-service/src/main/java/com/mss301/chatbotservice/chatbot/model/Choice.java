package com.mss301.chatbotservice.chatbot.model;

import lombok.Data;

@Data
public class Choice {
    private Message message;
    private String finish_reason;
    private String native_finish_reason;
    private int index;
    private Object logprobs;
}
