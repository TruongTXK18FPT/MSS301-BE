package com.mss301.contentservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mss301.contentservice.entity.QuizQuestion;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByQuizIdOrderByIdAsc(Long quizId);
}
