package com.mss301.authservice.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request DTO for teacher approval/rejection by admin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeacherApprovalRequest {
    String action; // APPROVE or REJECT
    String rejectionReason; // Optional, only needed for REJECT
}
