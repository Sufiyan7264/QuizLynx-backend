package com.projectJava.quizApp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuizSummary {

    private Long id;
    private String title;
    private Integer totalQuestions;
}
