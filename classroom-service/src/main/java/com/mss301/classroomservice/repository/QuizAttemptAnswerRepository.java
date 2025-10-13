package com.mss301.classroomservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mss301.classroomservice.entity.QuizAttemptAnswer;

public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswer, Long> {
    List<QuizAttemptAnswer> findByAttemptId(Long attemptId);
}
