package com.projectJava.quizApp.DTO;
import lombok.Data;

@Data
public class AiGenerateRequest {
    private Long quizId;
    private String contentText; // The paragraph
    private int numberOfQuestions; // e.g., 5
}