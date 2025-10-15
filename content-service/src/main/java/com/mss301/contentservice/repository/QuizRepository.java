package com.mss301.contentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mss301.contentservice.entity.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, Long> {}
