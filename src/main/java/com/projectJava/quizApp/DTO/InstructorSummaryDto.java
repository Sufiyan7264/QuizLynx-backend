package com.projectJava.quizApp.DTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InstructorSummaryDto {
    private String id;
    private String name;
    private String email;
    private String subject;
    private int totalQuizzes;   // Quizzes assigned by this instructor
    private double averageScore; // My average in this instructor's class
    private String lastActive;
}