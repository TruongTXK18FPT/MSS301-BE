package com.mss301.mindmapservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mindmap_nodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MindmapNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mindmap_id", nullable = false)
    private Long mindmapId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false)
    private NodeType nodeType;

    @Column(name = "position_x")
    private Double positionX;

    @Column(name = "position_y")
    private Double positionY;

    @Column(name = "width")
    private Double width;

    @Column(name = "height")
    private Double height;

    @Column(name = "color")
    private String color;

    @Column(name = "background_color")
    private String backgroundColor;

    @Column(name = "border_color")
    private String borderColor;

    @Column(name = "font_size")
    private Integer fontSize;

    @Column(name = "font_family")
    private String fontFamily;

    @Column(name = "is_bold")
    private Boolean isBold = false;

    @Column(name = "is_italic")
    private Boolean isItalic = false;

    @Column(name = "is_underline")
    private Boolean isUnderline = false;

    @Column(name = "parent_node_id")
    private Long parentNodeId;

    @Column(name = "level")
    private Integer level = 0;

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    @Column(name = "is_collapsed")
    private Boolean isCollapsed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mindmap_id", insertable = false, updatable = false)
    private Mindmap mindmap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_node_id", insertable = false, updatable = false)
    private MindmapNode parentNode;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum NodeType {
        ROOT("Root"),
        TOPIC("Topic"),
        SUBTOPIC("Subtopic"),
        CONCEPT("Concept"),
        EXAMPLE("Example"),
        EXERCISE("Exercise"),
        FORMULA("Formula"),
        DEFINITION("Definition"),
        THEOREM("Theorem"),
        PROOF("Proof");

        private final String displayName;

        NodeType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
