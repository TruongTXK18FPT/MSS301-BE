package com.mss301.contentservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mss301.contentservice.entity.QuizAttempt;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByQuizId(Long quizId);

    List<QuizAttempt> findByQuizIdAndStudentId(Long quizId, Long studentId);

    List<QuizAttempt> findByStudentId(Long studentId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.quizId = :quizId ORDER BY qa.startedAt DESC")
    List<QuizAttempt> findByQuizIdOrderByStartedAtDesc(@Param("quizId") Long quizId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.quizId = :quizId AND qa.studentId = :studentId ORDER BY qa.startedAt DESC")
    List<QuizAttempt> findByQuizIdAndStudentIdOrderByStartedAtDesc(
            @Param("quizId") Long quizId,
            @Param("studentId") Long studentId);

    Optional<QuizAttempt> findFirstByQuizIdAndStudentIdAndSubmittedAtIsNullOrderByStartedAtDesc(
            Long quizId, Long studentId);

    long countByQuizId(Long quizId);

    long countByQuizIdAndSubmittedAtIsNotNull(Long quizId);
}
