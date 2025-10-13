package com.mss301.classroomservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mss301.classroomservice.entity.QuizAttempt;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {}
