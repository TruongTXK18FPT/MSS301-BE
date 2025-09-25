package com.mss301.notificationservice.dto.request;

import java.util.Map;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailRequest {
    String to;
    String subject;
    String templateName;
    Map<String, Object> templateData;
}
