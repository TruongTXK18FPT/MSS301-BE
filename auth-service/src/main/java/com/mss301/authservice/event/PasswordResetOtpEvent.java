package com.mss301.authservice.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PasswordResetOtpEvent {
    String userEmail;
    String otpCode;
    Integer expiryMinutes;
    String subject;
}
