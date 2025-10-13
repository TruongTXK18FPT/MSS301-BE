package com.mss301.authservice.dto.response;

import java.util.Date;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationResponse {
    String token;
    Date expiryTime;
    boolean authenticated;
    // ADDED: Optional fields for Google-first login when registration is required
    String email;
    String name;
    String givenName;
    String familyName;
    String picture;
}
