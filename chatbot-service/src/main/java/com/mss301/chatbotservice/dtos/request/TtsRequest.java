package com.mss301.chatbotservice.dtos.request;

import com.mss301.ragservice.enums.ResponseMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TtsRequest {
    private String text;
    private ResponseMode mode;
}

