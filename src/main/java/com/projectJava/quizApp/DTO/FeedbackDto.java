package com.projectJava.quizApp.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeedbackDto {
    private String id;
    private String instructorName;
    private String quizTitle;
    private String feedback;
    private String date;
    private Integer rating; // 1-5
}