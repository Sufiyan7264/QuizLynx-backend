package com.projectJava.quizApp.DTO;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StudentResultItemDto {
    private Long studentId;
    private String studentName;
    private String studentEmail;

    private Double score;
    private Double totalMarks;
    private Double percentage;

    private LocalDateTime submittedAt;
    private Long timeTaken; // in seconds

    private Boolean passed;
}