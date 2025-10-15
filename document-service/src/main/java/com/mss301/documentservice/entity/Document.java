package com.mss301.documentservice.entity;

import com.mss301.documentservice.entity.enums.DocumentStatus;
import com.mss301.documentservice.entity.enums.Language;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@org.springframework.data.elasticsearch.annotations.Document(indexName = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {
    @Id
    private String id;

    private String title;
    private String fileName;
    private String filePath;
    private DocumentStatus status;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime uploadedAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime processedAt;

    private Long size; // Tính theo bytes
    private Language language;
    private String contentType;
    private Integer totalPages;
    private String description;
}
