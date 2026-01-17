package com.projectJava.quizApp.DTO;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class QuizSummaryItemDto {
    private String id;
    private String title;
    private String instructorName;
    private String subject;
    private LocalDate dueDate;
    private String status; // Completed, Pending, Overdue
    private String score;
    private String maxScore;
}