package com.mss301.contentservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "content_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentItem {

    public enum Type {
        LESSON,
        ASSIGNMENT,
        QUIZ,
        RESOURCE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 100)
    private String subject; // e.g., Math

    @Column(length = 50)
    private String grade; // e.g., Grade 6

    @Column(length = 500)
    private String tags; // comma-separated tags

    private Boolean isPublic = false;

    // Classroom association
    private Long classroomId;

    // For assignments
    private LocalDateTime dueDate;
    
    @Column(nullable = true)
    private Integer totalPoints;

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
