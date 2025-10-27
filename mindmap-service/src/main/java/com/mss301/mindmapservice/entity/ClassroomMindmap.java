package com.mss301.mindmapservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "classroom_mindmaps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomMindmap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mindmap_id", nullable = false)
    private Long mindmapId;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "shared_at", nullable = false)
    private LocalDateTime sharedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mindmap_id", insertable = false, updatable = false)
    private Mindmap mindmap;

    @PrePersist
    protected void onCreate() {
        sharedAt = LocalDateTime.now();
    }
}
