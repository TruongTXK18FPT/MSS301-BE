package com.mss301.mindmapservice.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mindmaps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Mindmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "grade")
    private String grade;

    @Column(name = "subject")
    private String subject;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Column(name = "is_ai_generated", nullable = false)
    private Boolean isAiGenerated = false;

    @Column(name = "ai_provider")
    private String aiProvider; // mistral, gemini

    @Column(name = "ai_model")
    private String aiModel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "access_count")
    private Integer accessCount = 0;

    @Column(name = "favorite_count")
    private Integer favoriteCount = 0;

    @Column(name = "share_count")
    private Integer shareCount = 0;

    @OneToMany(mappedBy = "mindmap", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MindmapNode> nodes;

    @OneToMany(mappedBy = "mindmap", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MindmapEdge> edges;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void incrementAccessCount() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public void incrementFavoriteCount() {
        this.favoriteCount++;
    }

    public void incrementShareCount() {
        this.shareCount++;
    }
}
