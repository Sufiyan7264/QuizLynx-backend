package com.projectJava.quizApp.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class StudentDto {
    private Long id;
    private String name;    // or username
    private String email;
    private String notes;   // "Joined Class: Class 8"
    private boolean isActive;
    private Double averageScore;

    public StudentDto(Long id, String username, String email) {
    }
}