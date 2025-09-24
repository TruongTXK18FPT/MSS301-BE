package com.mss301.authservice.dto.response;

import java.time.LocalDateTime;

import com.mss301.authservice.entity.UserAccount;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String id;
    String email;
    boolean noPassword;
    UserAccount.UserStatus status;
    boolean emailVerified;
    String tenantId;
    LocalDateTime createdAt;
    LocalDateTime lastLoginAt;
}
