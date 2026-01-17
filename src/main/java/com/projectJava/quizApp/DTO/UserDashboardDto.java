package com.projectJava.quizApp.DTO;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UserDashboardDto {
    // 1. Stat Cards
    private long totalQuizzesTaken;
    private String averageScore;    // String to include "%"
    private int completedQuizzes;
    private String currentStreak;   // e.g. "5 days"

    // 2. For "Resume Last Quiz" button
    private Long lastActiveQuizId;  // Null if no active quiz

    // 3. For Pie Chart (Performance Breakdown)
    private long excellentCount; // 90-100%
    private long goodCount;      // 75-89%
    private long averageCount;   // 60-74%
    private long poorCount;      // < 60%

    // 4. Recent History Table
    private List<UserQuizHistoryDto> recentHistory;
}