package com.mss301.contentservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long quizId;

    @Column(nullable = false)
    private Long studentId;

    @Column(length = 100)
    private String studentName;

    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = true)
    private String answers; // JSON string of answers (null when not submitted)

    @PrePersist
    void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        // Ensure answers is null (not empty string) when creating new attempt
        if (answers != null && answers.trim().isEmpty()) {
            answers = null;
        }
    }
}
