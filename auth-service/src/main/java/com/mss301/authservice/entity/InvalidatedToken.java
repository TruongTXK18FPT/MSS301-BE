package com.mss301.authservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "invalidated_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvalidatedToken {
    @Id
    String id;

    @Column(nullable = false)
    LocalDateTime expiryTime;

    @Column(nullable = false)
    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();
}
