package com.mss301.contentservice.entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long quizId; // references Quiz.contentItemId

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column
    private Integer points;

    @Column(length = 30)
    private String type; // MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER, ESSAY
    
    @Column(columnDefinition = "TEXT")
    private String explanation;
    
    @Column
    private Integer orderIndex;
}
