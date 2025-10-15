package com.mss301.contentservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mss301.contentservice.entity.QuizOption;

public interface QuizOptionRepository extends JpaRepository<QuizOption, Long> {
    List<QuizOption> findByQuestionIdOrderByIdAsc(Long questionId);
}
