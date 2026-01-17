package com.projectJava.quizApp.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data

public class CreateInstructorProfileRequest {

    @NotBlank @Size(max = 100)
    private String displayName;

    @Size(max = 2000)
    private String bio;

    private List<String> subjects;
    private String avatarUrl;
}
