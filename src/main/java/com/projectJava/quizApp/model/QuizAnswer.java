package com.projectJava.quizApp.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "quiz_answers")
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link back to the parent submission
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private QuizSubmission submission;

    // Link to the specific Question
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    // What the student actually clicked/typed
    private String selectedOption;
}