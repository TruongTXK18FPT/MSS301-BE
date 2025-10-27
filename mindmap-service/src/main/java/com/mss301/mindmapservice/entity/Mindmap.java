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

    @Column(name = "visibility")
    @Enumerated(EnumType.STRING)
    private Visibility visibility = Visibility.PRIVATE;

    @Column(name = "is_ai_generated", nullable = false)
    private Boolean isAiGenerated = false;

    @Column(name = "owner_role")
    @Enumerated(EnumType.STRING)
    private OwnerRole ownerRole = OwnerRole.STUDENT;

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

    @Column(name = "color", length = 100)
    private String color; // Theme color for UI (e.g., "from-purple-500 to-pink-500")

    @Column(name = "difficulty")
    private String difficulty; // easy, medium, hard, mixed

    @Column(name = "cognitive_level")
    private String cognitiveLevel; // nhan-biet, thong-hieu, van-dung, van-dung-cao

    @Column(name = "estimated_time")
    private String estimatedTime; // e.g., "30 phút"

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // Comma-separated tags

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

    public enum Visibility {
        PRIVATE("Riêng tư"),
        PUBLIC("Công khai"),
        CLASSROOM("Lớp học");

        private final String displayName;

        Visibility(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum OwnerRole {
        STUDENT("Học sinh"),
        TEACHER("Giáo viên");

        private final String displayName;

        OwnerRole(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
