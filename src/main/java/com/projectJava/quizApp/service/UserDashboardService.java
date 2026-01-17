package com.projectJava.quizApp.service;

import com.projectJava.quizApp.DTO.UserDashboardDto;
import com.projectJava.quizApp.DTO.UserQuizHistoryDto;
import com.projectJava.quizApp.model.QResult;
import com.projectJava.quizApp.repo.QResultRepo;
import com.projectJava.quizApp.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserDashboardService {

    @Autowired
    private QResultRepo qResultRepo;

    @Autowired
    private UserRepo userRepo;

    public UserDashboardDto getUserDashboardData(String username) {
        // 1. Fetch ALL results for this user (Sorted by date desc)
        List<QResult> allResults = qResultRepo.findByStudent_UsernameOrderBySubmitDateDesc(username);

        // 2. Separate Completed vs In-Progress
        List<QResult> completed = allResults.stream()
                .filter(r -> r.getSubmitDate() != null)
                .collect(Collectors.toList());

        List<QResult> inProgress = allResults.stream()
                .filter(r -> r.getSubmitDate() == null)
                .collect(Collectors.toList());

        // --- STATS CALCULATION ---
        long totalTaken = allResults.size();
        int completedCount = completed.size();

        double avgScoreVal = 0.0;
        if (completedCount > 0) {
            double totalPercentage = completed.stream()
                    .mapToDouble(this::calculatePercentage)
                    .sum();
            avgScoreVal = totalPercentage / completedCount;
        }
        String avgScoreStr = String.format("%.1f%%", avgScoreVal);

        // --- STREAK CALCULATION (Simplified) ---
        // Checks consecutive days of activity
        int streak = calculateStreak(completed);

        // --- CHART DATA (Performance Breakdown) ---
        long excellent = completed.stream().filter(r -> calculatePercentage(r) >= 90).count();
        long good = completed.stream().filter(r -> calculatePercentage(r) >= 75 && calculatePercentage(r) < 90).count();
        long average = completed.stream().filter(r -> calculatePercentage(r) >= 60 && calculatePercentage(r) < 75).count();
        long poor = completed.stream().filter(r -> calculatePercentage(r) < 60).count();

        // --- RESUME LOGIC ---
        // If there is an in-progress quiz, get the ID of the most recent one
        Long resumeQuizId = null;
        if (!inProgress.isEmpty()) {
            resumeQuizId = inProgress.get(0).getQuiz().getId();
        }

        // --- HISTORY MAPPING (Top 5) ---
        List<UserQuizHistoryDto> history = allResults.stream()
                .limit(5)
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());

        return UserDashboardDto.builder()
                .totalQuizzesTaken(totalTaken)
                .completedQuizzes(completedCount)
                .averageScore(avgScoreStr)
                .currentStreak(streak + " days")
                .lastActiveQuizId(resumeQuizId)
                .excellentCount(excellent)
                .goodCount(good)
                .averageCount(average)
                .poorCount(poor)
                .recentHistory(history)
                .build();
    }

    // Helper: Calculate Percentage
    private double calculatePercentage(QResult r) {
        if (r.getTotalMarks() == 0) return 0.0;
        return (r.getScoreObtained().doubleValue() / r.getTotalMarks()) * 100.0;
    }

    // Helper: Map Entity to History DTO
    private UserQuizHistoryDto mapToHistoryDto(QResult r) {
        boolean isCompleted = r.getSubmitDate() != null;
        String scoreDisplay = isCompleted ?
                Math.round(r.getScoreObtained().doubleValue()) + "/" + r.getTotalMarks() :
                "-";

        return UserQuizHistoryDto.builder()
                .subject(r.getQuiz().getSubject())
                .score(scoreDisplay)
                .status(isCompleted ? "Completed" : "In Progress")
                .date(isCompleted ? r.getSubmitDate().toLocalDate().toString() : r.getStartTime().toLocalDate().toString())
                .quizId(r.getQuiz().getId())
                .build();
    }

    // Helper: Simple Streak Calculation
    private int calculateStreak(List<QResult> results) {
        if (results.isEmpty()) return 0;

        // Get unique dates sorted descending
        List<LocalDate> dates = results.stream()
                .map(r -> r.getSubmitDate().toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        int streak = 0;
        LocalDate current = LocalDate.now();

        // If user didn't take quiz today or yesterday, streak is broken
        if (dates.isEmpty() || (!dates.get(0).equals(current) && !dates.get(0).equals(current.minusDays(1)))) {
            return 0;
        }

        // Count backwards
        for (LocalDate date : dates) {
            if (date.equals(current)) {
                // Took quiz today, count it
                streak++;
                current = current.minusDays(1);
            } else if (date.equals(current.minusDays(1))) {
                // Took quiz yesterday, count it
                streak++;
                current = current.minusDays(1);
            } else {
                break; // Gap found
            }
        }
        return streak;
    }
}