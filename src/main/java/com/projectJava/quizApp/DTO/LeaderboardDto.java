package com.projectJava.quizApp.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class LeaderboardDto {
    private int rank;
    private String name;
    private Long score;
    private String country;
    private Long id;
    public LeaderboardDto(String name, Long score, String country) {
        this.name = name;
        this.score = score;
        this.country = (country != null) ? country : "Global"; // Handle null country
    }
}
