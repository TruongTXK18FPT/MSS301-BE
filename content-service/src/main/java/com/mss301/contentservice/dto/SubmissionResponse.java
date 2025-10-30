package com.mss301.contentservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.mss301.contentservice.entity.Submission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResponse {
    private Long id;
    private Long assignmentId;
    private Long studentId;
    private String studentName;
    private String content;
    private String fileIds;
    private LocalDateTime submittedAt;
    private BigDecimal grade;
    private String feedback;
    private LocalDateTime gradedAt;
    private Long gradedBy;
    private Submission.Status status;
}
