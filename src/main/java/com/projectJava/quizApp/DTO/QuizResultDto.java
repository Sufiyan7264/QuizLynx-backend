package com.projectJava.quizApp.DTO;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuizResultDto {
    // 1. IDs
    private Long id;           // The Submission ID (Required for URL: /result/55)
    private Long quizId;       // The Quiz ID
    private String topic;
    // 2. Info
    private String quizTitle;

    // 3. Scoring (Using Double for calculations, Integer is also fine but Service uses Double)
    private Double score;      // Matches frontend 'score'
    private Double totalMarks;
    private Double percentage;

    // 4. Status & Time
    private Boolean passed;    // Matches frontend 'passed'
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Long timeTaken;    // in seconds

    // 5. Detailed Review
    private List<QuestionReviewDto> questionReviews;

    // --- Optional: Keep these if you have other legacy code using them ---
     private Integer scoreObtained; // mapped to 'score' now
     private String resultStatus;   // mapped to 'passed' now
     private String feedback;
}