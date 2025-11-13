package com.mss301.chatbotservice.chatbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private String code;
    private MetaData metaData;
}
class MetaData {
    private String raw;
    private String provider_name;
}