package com.projectJava.quizApp.DTO;

import lombok.Data;
import java.util.List;

@Data
public class BatchQuizResultDto {
    // Summary Card Data
    private String quizTitle;
    private Long totalStudents;      // Total enrolled in batch
    private Integer studentsCompleted; // How many submitted
    private Double averagePercentage;

    // The Table Data
    private List<StudentResultItemDto> studentResults;
}