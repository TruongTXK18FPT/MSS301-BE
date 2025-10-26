package com.mss301.profileservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mss301.profileservice.entity.StudentGuardian;
import com.mss301.profileservice.entity.StudentGuardianId;

@Repository
public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, StudentGuardianId> {
    List<StudentGuardian> findByStudentId(Long studentId);

    List<StudentGuardian> findByGuardianId(Long guardianId);

    Optional<StudentGuardian> findByStudentIdAndGuardianId(Long studentId, Long guardianId);
}
