package com.mss301.authservice.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Event published when admin approves/rejects teacher registration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherApprovalEvent {
    String userId;
    String email;
    String approvalStatus; // APPROVED or REJECTED
    String rejectionReason; // Optional, only for REJECTED
}
