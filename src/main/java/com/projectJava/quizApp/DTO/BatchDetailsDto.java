package com.projectJava.quizApp.DTO;

import com.projectJava.quizApp.model.QuizSummary;
import lombok.Data;
import java.util.List;

@Data
public class BatchDetailsDto {
    private Long id;
    private String batchName;
    private String batchCode;
    private String description;

    // The lists we were missing
    private List<StudentDto> students;
    private List<QuizSummary> quizzes;
}