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

    private Long contentId; // reference to content-service ContentItem (nullable for lessons)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentType type;

    // Embedded content fields (for lessons/direct content)
    private String title;
    
    @Column(length = 1000)
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String content; // Lesson content stored directly

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
