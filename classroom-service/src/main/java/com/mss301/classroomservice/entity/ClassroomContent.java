package com.mss301.classroomservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "classroom_contents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomContent {

    public enum ContentType {
        LESSON,
        ASSIGNMENT,
        QUIZ,
        RESOURCE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long classroomId;

    @Column(nullable = false)
    private Long contentId; // reference to content-service ContentItem

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType type;

    private Boolean visible = true;
    private Integer orderIndex;

    private LocalDateTime publishAt;
    private LocalDateTime dueAt;
    private Integer maxPoints;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
