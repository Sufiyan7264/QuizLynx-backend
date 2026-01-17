package com.projectJava.quizApp.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePassRequest {
    @NotBlank
    private String oldPassword;
    @NotBlank
    @Size(min = 8,message = "new password must be at least 8 characters")
    private String newPassword;

}
