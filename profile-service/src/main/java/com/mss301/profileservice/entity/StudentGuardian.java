package com.mss301.profileservice.entity;

import java.io.Serializable;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id", insertable = false, updatable = false)
    private GuardianProfile guardianProfile;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
class StudentGuardianId implements Serializable {
    private Long studentId;
    private Long guardianId;
}
