package com.mss301.profileservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_guardians")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(StudentGuardianId.class)
public class StudentGuardian {

    @Id
    @Column(name = "student_id")
    private Long studentId;

    @Id
    @Column(name = "guardian_id")
    private Long guardianId;

    @Column(name = "relationship")
    private String relationship;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Removed @ManyToOne relationships to avoid foreign key constraint issues
    // Relationships are handled at application level via IDs
    @Transient
    private StudentProfile studentProfile;

    @Transient
    private GuardianProfile guardianProfile;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
