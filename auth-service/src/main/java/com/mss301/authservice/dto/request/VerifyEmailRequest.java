package com.mss301.authservice.dto.request;

import jakarta.validation.constraints.Email;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerifyEmailRequest {
    @Email
    String email;

    String otpCode;
}
