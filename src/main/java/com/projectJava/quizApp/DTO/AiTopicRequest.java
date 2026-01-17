package com.projectJava.quizApp.DTO;

import lombok.Data;

@Data
public class AiTopicRequest {
    private String topic;
    private String difficulty; // "Easy", "Medium", "Hard"
    private int questions;     // e.g. 10
}