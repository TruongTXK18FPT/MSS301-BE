package com.mss301.authservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileStatusResponse {
    Boolean profileCompleted;
    String userType;
    String username;
    String email;
}
