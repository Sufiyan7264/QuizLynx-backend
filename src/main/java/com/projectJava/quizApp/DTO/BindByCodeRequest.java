package com.projectJava.quizApp.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BindByCodeRequest {
    @NotBlank
    private String code;
}
