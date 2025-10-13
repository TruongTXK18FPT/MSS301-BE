package com.mss301.mindmapservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "mindmap_content_links",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mindmap_id", "content_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MindmapContentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mindmap_id", nullable = false)
    private Long mindmapId;

    @Column(name = "content_id", nullable = false)
    private Long contentId; // content-service ContentItem id

    @Column(length = 50)
    private String contentType; // LESSON/ASSIGNMENT/QUIZ/RESOURCE

    @Column(length = 255)
    private String note;

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
