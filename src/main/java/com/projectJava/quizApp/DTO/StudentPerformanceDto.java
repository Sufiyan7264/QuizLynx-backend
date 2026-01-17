package com.projectJava.quizApp.DTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentPerformanceDto {
    private String name;
    private double averageScore; // Mapped to 'score' for attention list
    private int quizzesCompleted;
    private String status; // "Excellent", "Good", "Average"
}