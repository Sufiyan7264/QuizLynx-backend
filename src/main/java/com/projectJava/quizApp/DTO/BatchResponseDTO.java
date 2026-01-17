package com.projectJava.quizApp.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchResponseDTO{
    private Long id;
    private String batchName;
    private String batchCode;
    private String description;
    private long studentCount;
    private long quizCount;
}