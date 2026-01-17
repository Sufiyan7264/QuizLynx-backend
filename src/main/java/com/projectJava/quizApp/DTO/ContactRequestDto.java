package com.projectJava.quizApp.DTO;

import lombok.Data;

@Data
public class ContactRequestDto {

    private String name;
    private String email;
    private String subject;
    private String message;
}
