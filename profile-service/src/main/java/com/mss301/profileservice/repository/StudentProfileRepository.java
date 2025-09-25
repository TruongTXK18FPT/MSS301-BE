package com.mss301.profileservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mss301.profileservice.entity.StudentProfile;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUserId(Long userId);

    List<StudentProfile> findByGrade(String grade);

    List<StudentProfile> findBySchool(String school);

    boolean existsByUserId(Long userId);
}
