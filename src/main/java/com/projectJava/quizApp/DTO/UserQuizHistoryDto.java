package com.projectJava.quizApp.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserQuizHistoryDto {
    private String subject;
    private String score;       // Format: "8/10"
    private String status;      // "Completed" or "In Progress"
    private String date;        // "2023-10-27"
    private Long quizId;        // To allow clicking on it
}