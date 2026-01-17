package com.projectJava.quizApp.DTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuickStatsDto {
    private long totalStudents;
    private long topPerformers;
    private String averageCompletionTime;
    private double averageScore;
}