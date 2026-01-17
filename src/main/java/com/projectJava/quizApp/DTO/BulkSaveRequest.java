package com.projectJava.quizApp.DTO;

import lombok.Data;

import java.util.List;

@Data
public class BulkSaveRequest {
    private Long quizId;
    private List<QuestionDto> questions;
}
