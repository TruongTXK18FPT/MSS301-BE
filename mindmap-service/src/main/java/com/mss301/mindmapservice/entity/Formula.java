package com.mss301.mindmapservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "formulas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Formula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", nullable = false)
    private Long nodeId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "formula_text", nullable = false, columnDefinition = "TEXT")
    private String formulaText; // LaTeX format

    @Column(name = "formula_latex", columnDefinition = "TEXT")
    private String formulaLatex; // Pure LaTeX for rendering

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "usage_example", columnDefinition = "TEXT")
    private String usageExample;

    @Column(name = "variables", columnDefinition = "TEXT")
    private String variables; // JSON: [{"symbol": "a", "meaning": "cạnh"}]

    @Column(name = "conditions", columnDefinition = "TEXT")
    private String conditions; // Điều kiện áp dụng

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    @Column(name = "is_primary")
    private Boolean isPrimary = false; // Công thức chính/phụ

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
