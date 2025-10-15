package com.mss301.documentservice.entity;

import com.mss301.documentservice.entity.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "processing_jobs")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProcessingJob {
    @Id
    private String id;
    private String documentId;
    private JobStatus status;
    private String currentStep;
    private Integer progress;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime startedAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime completedAt;

    private String errorMessage;

    private String errorDetails;
    private Integer totalPages;
    private Integer processedPages;
    private Boolean usedOcr;
    private Long processingTimeMs;
}
