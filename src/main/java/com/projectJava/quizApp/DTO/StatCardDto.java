package com.projectJava.quizApp.DTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatCardDto {
    private String title;
    private String value; // String to handle "87%" or "156"
    private double change;
    private String changeType; // "increase" or "decrease"
    private String period;
}