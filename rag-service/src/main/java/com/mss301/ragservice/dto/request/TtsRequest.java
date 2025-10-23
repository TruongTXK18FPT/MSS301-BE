package com.mss301.ragservice.dto.request;

import com.mss301.ragservice.enums.ResponseMode;
import lombok.Data;

@Data
public class TtsRequest {
    private String text;
    private ResponseMode mode;
}
