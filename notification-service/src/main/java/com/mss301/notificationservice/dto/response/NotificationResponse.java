package com.mss301.notificationservice.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private String id;
    private String recipientId;
    private String recipientEmail;
    private String title;
    private String message;
    private String type;
    private String status; // PENDING, SENT, FAILED
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
