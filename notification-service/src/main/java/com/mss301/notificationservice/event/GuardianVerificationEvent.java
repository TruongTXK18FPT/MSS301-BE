package com.mss301.notificationservice.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GuardianVerificationEvent {
    String studentEmail;
    String studentName;
    String guardianName;
    String guardianEmail;
    String verificationCode;
    String relationship;
    Integer expiryMinutes;
    String subject;
}
