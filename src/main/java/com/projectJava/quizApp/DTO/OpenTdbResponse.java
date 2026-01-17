package com.projectJava.quizApp.DTO;

import lombok.Data;
import java.util.List;

@Data
public class OpenTdbResponse {
    private int response_code;
    private List<ExternalQuestion> results;

    @Data
    public static class ExternalQuestion {
        private String category;
        private String type; // "multiple" or "boolean"
        private String difficulty;
        private String question;
        private String correct_answer;
        private List<String> incorrect_answers;
    }
}