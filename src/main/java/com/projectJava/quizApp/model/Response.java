package com.projectJava.quizApp.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Response {

    private Long id;
    private String  response;
}
