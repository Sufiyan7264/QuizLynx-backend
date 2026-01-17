package com.projectJava.quizApp.DTO;

import com.projectJava.quizApp.enums.QuizStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QuizDto {
    private Long id;
    private String title;
    private String description;
    private String subject;       // NEW
    private Integer timerInMin;
    private Integer totalMarks;   // NEW
    private Integer passingMarks; // NEW

    private LocalDateTime startDate; // NEW
    private LocalDateTime endDate;   // NEW
    private QuizStatus status;       // NEW (DRAFT, PUBLISHED, CLOSED)

    private Long batchId; // To link/display batch info
    private int questionCount; // Helper for UI
}