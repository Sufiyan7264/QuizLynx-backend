package com.projectJava.quizApp.DTO;

import lombok.Data;

@Data
public class QuestionReviewDto {
    private String questionId;
    private String questionText;

    // Options needed for getReviewOptions()
    private String option1;
    private String option2;
    private String option3;
    private String option4;

    // Answer checking
    private String selectedAnswer;  // The text of what user picked
    private String correctAnswer;   // The text of the right answer

    // Scoring & Status
    private boolean isCorrect;      // review.isCorrect
    private Double marks;           // Total marks for this question
    private Double earnedMarks;     // Marks the user actually got (0 or full)
    private String explanation;     // Optional, if you use it
}
