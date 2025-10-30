package com.mss301.profileservice.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherApprovalEvent {
    String userId;
    String email;
    String approvalStatus; // "APPROVED" or "REJECTED"
    String rejectionReason; // Optional
}
