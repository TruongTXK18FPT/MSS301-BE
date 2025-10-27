package com.mss301.notificationservice.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Event received when admin approves/rejects teacher registration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherApprovalEvent {
    String userId;
    String email;
    String approvalStatus; // APPROVED or REJECTED
    String rejectionReason; // Optional, only for REJECTED
}
