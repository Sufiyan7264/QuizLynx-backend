package com.projectJava.quizApp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@Table(name = "submitted_answers")
public class SubmittedAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to the parent attempt (QResult)
    @ManyToOne
    @JoinColumn(name = "result_id")
    private QResult result;

    // Link to the specific Question
    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    // The actual answer the student gave (e.g., "Java", "Option A")
    @Column(length = 500) // generous length for text answers
    private String selectedResponse;
}