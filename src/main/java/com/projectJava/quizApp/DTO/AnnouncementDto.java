package com.projectJava.quizApp.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnnouncementDto {
    private String id;
    private String instructorName;
    private String title;
    private String message;
    private String date; // String to match frontend (ISO format or formatted)
    private String priority; // "high", "medium", "low"
}