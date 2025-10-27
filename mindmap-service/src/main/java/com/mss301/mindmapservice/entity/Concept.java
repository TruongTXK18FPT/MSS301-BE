package com.mss301.mindmapservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "concepts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Concept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", nullable = false)
    private Long nodeId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "definition", nullable = false, columnDefinition = "TEXT")
    private String definition;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation; // Giải thích chi tiết

    @Column(name = "key_points", columnDefinition = "TEXT")
    private String keyPoints; // JSON array of key points

    @Column(name = "examples", columnDefinition = "TEXT")
    private String examples; // JSON array of examples

    @Column(name = "common_mistakes", columnDefinition = "TEXT")
    private String commonMistakes; // JSON array of common mistakes

    @Column(name = "tips", columnDefinition = "TEXT")
    private String tips; // Mẹo ghi nhớ

    @Column(name = "prerequisites", columnDefinition = "TEXT")
    private String prerequisites; // Kiến thức tiên quyết

    @Column(name = "related_concepts", columnDefinition = "TEXT")
    private String relatedConcepts; // Các khái niệm liên quan

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", insertable = false, updatable = false)
    private MindmapNode node;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
