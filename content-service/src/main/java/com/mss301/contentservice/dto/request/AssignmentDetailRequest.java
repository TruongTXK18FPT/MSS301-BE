package com.mss301.contentservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDetailRequest {
    private String instructions;
    private String submissionType; // TEXT, FILE, BOTH
    private String attachmentFileIds; // csv
}
