package com.mss301.authservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OTPResponse {
    String email;
    LocalDateTime expiryTime;
    int expiryInSeconds; // Remaining seconds until expiry
}
