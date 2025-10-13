package com.mss301.classroomservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mss301.classroomservice.entity.Grade;

public interface GradeRepository extends JpaRepository<Grade, Long> {}
