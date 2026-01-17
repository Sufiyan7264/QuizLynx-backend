package com.projectJava.quizApp.DTO;

import lombok.Data;
import java.util.List;

@Data
public class StudentBatchDto {
    private Long id;
    private String batchName;
    private String description;
    private String instructorName; // Just the name, not full profile

    // The student only needs to see the quizzes they can take
//    private List<QuizDto> activeQuizzes;
}