package com.mss301.mindmapservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mindmap_edges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MindmapEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mindmap_id", nullable = false)
    private Long mindmapId;

    @Column(name = "from_node_id", nullable = false)
    private Long fromNodeId;

    @Column(name = "to_node_id", nullable = false)
    private Long toNodeId;

    @Column(name = "relationship_type")
    private String relationshipType;

    @Column(name = "label")
    private String label;

    @Column(name = "color")
    private String color;

    @Column(name = "thickness")
    private Integer thickness = 2;

    @Column(name = "style")
    private String style; // solid, dashed, dotted

    @Column(name = "is_directed", nullable = false)
    private Boolean isDirected = true;

    @Column(name = "weight")
    private Double weight = 1.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mindmap_id", insertable = false, updatable = false)
    private Mindmap mindmap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_node_id", insertable = false, updatable = false)
    private MindmapNode fromNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_node_id", insertable = false, updatable = false)
    private MindmapNode toNode;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum RelationshipType {
        PARENT_CHILD("Parent-Child"),
        RELATED("Related"),
        DEPENDS_ON("Depends On"),
        LEADS_TO("Leads To"),
        CONTAINS("Contains"),
        PART_OF("Part Of"),
        SIMILAR_TO("Similar To"),
        OPPOSITE_OF("Opposite Of");

        private final String displayName;

        RelationshipType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
