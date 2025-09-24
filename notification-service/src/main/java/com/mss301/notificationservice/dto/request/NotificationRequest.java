package com.mss301.notificationservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {
    private String recipientId;
    private String recipientEmail;
    private String title;
    private String message;
    private String type; // EMAIL, SMS, PUSH
    private Object templateData;
    private String templateName;
}
