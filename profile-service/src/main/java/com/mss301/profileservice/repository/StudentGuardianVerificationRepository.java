package com.mss301.profileservice.repository;

import com.mss301.profileservice.entity.StudentGuardianVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentGuardianVerificationRepository extends JpaRepository<StudentGuardianVerification, Long> {

    Optional<StudentGuardianVerification> findByGuardianIdAndStudentIdAndVerificationCode(
            Long guardianId, Long studentId, String verificationCode);

    Optional<StudentGuardianVerification> findByGuardianIdAndStudentId(
            Long guardianId, Long studentId);

    Optional<StudentGuardianVerification> findByVerificationCode(String verificationCode);
}
