package com.projectJava.quizApp.service;

import com.projectJava.quizApp.DTO.*;
import com.projectJava.quizApp.model.*;
import com.projectJava.quizApp.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Locale;

@Service
public class DashboardService {

    @Autowired private AssignmentRepo assignmentRepo;
    @Autowired private QResultRepo qResultRepo;
    @Autowired private QuizRepo quizRepo;

    public DashboardResponseDto getDashboardData(String username) {
        DashboardResponseDto response = new DashboardResponseDto();

        // 1. Fetch Raw Data
        long totalStudents = assignmentRepo.countStudentsByInstructor(username);
        List<QResult> allResults = qResultRepo.findAllByInstructor(username);

        // 2. Calculate Global Averages
        double overallSum = allResults.stream()
                .mapToDouble(r -> (r.getTotalMarks() > 0) ? (r.getScoreObtained().doubleValue() / r.getTotalMarks()) * 100 : 0)
                .sum();
        double overallAvg = allResults.isEmpty() ? 0 : overallSum / allResults.size();
        overallAvg = Math.round(overallAvg * 10.0) / 10.0;

        // 3. Process Student Performance (Top & Attention)
        List<StudentPerformanceDto> studentStats = calculateStudentStats(allResults);

        // Sort for Top Students (High Score -> Low Score)
        List<StudentPerformanceDto> topStudents = studentStats.stream()
                .sorted(Comparator.comparingDouble(StudentPerformanceDto::getAverageScore).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // Sort for Attention Needed (Low Score -> High Score, threshold < 60%)
        List<StudentPerformanceDto> attentionNeeded = studentStats.stream()
                .filter(s -> s.getAverageScore() < 60)
                .sorted(Comparator.comparingDouble(StudentPerformanceDto::getAverageScore))
                .limit(5)
                .collect(Collectors.toList());

        List<Long> durationsInSeconds = allResults.stream()
                .filter(r -> r.getStartTime() != null && r.getSubmitDate() != null)
                .map(r -> java.time.Duration.between(r.getStartTime(), r.getSubmitDate()).toSeconds())
                .collect(Collectors.toList());

        String avgTimeString = "N/A";

        if (!durationsInSeconds.isEmpty()) {
            double avgSeconds = durationsInSeconds.stream().mapToLong(val -> val).average().orElse(0.0);
            long min = (long) (avgSeconds / 60);
            long sec = (long) (avgSeconds % 60);
            avgTimeString = min + " min " + sec + " sec";
        }

        // Set it in the QuickStats
        long topPerformerCount = 0;
        response.setQuickStats(QuickStatsDto.builder()
                .totalStudents(totalStudents)
                .topPerformers(topPerformerCount)
                .averageScore(overallAvg)
                .averageCompletionTime(avgTimeString) // <--- REAL VALUE
                .build());

        response.setTopStudents(topStudents);
        response.setStudentsNeedingAttention(attentionNeeded);

        // 4. Stat Cards
        response.setStats(buildStatCards(totalStudents, allResults, overallAvg));

        // 5. Quick Stats
        topPerformerCount = studentStats.stream().filter(s -> s.getAverageScore() >= 90).count();
        response.setQuickStats(QuickStatsDto.builder()
                .totalStudents(totalStudents)
                .topPerformers(topPerformerCount)
                .averageScore(overallAvg)
                .averageCompletionTime(avgTimeString) // Need startTime in entity to calc this
                .build());

        // 6. Charts
        response.setStudentEngagement(buildEngagementChart(username));
        response.setStudentPerformance(buildPerformanceBarChart(allResults));
        response.setQuizCompletion(buildCompletionPieChart(allResults, username)); // Simplified
        response.setPerformanceDistribution(buildDistributionChart(studentStats));

        return response;
    }

    // --- Helper Methods ---

    private List<StudentPerformanceDto> calculateStudentStats(List<QResult> results) {
        // Group results by Student ID
        Map<Long, List<QResult>> byStudent = results.stream()
                .collect(Collectors.groupingBy(r -> r.getStudent().getId()));

        List<StudentPerformanceDto> dtos = new ArrayList<>();

        for (Map.Entry<Long, List<QResult>> entry : byStudent.entrySet()) {
            List<QResult> studentResults = entry.getValue();
            if (studentResults.isEmpty()) continue;

            String name = studentResults.get(0).getStudent().getUsername(); // Or full name

            double totalPct = studentResults.stream()
                    .mapToDouble(r -> (r.getTotalMarks() > 0) ? (r.getScoreObtained().doubleValue()/r.getTotalMarks())*100 : 0)
                    .sum();
            double avg = totalPct / studentResults.size();

            String status = "Average";
            if(avg >= 90) status = "Excellent";
            else if(avg >= 75) status = "Good";

            dtos.add(StudentPerformanceDto.builder()
                    .name(name)
                    .averageScore(Math.round(avg * 10.0) / 10.0)
                    .quizzesCompleted(studentResults.size())
                    .status(status)
                    .build());
        }
        return dtos;
    }

    private List<StatCardDto> buildStatCards(long totalStudents, List<QResult> results, double avgScore) {
        List<StatCardDto> cards = new ArrayList<>();

        // Card 1: Total Students
        cards.add(StatCardDto.builder()
                .title("Total Students")
                .value(String.valueOf(totalStudents))
                .change(0) // Logic for "change" requires historical data
                .changeType("increase")
                .period("All time")
                .build());

        // Card 2: Active Students (Submitted in last 7 days)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long activeCount = results.stream()
                .filter(r -> r.getSubmitDate() != null) // <--- ADD THIS NULL CHECK
                .filter(r -> r.getSubmitDate().isAfter(sevenDaysAgo))
                .map(r -> r.getStudent().getId())
                .distinct()
                .count();

        cards.add(StatCardDto.builder()
                .title("Active Students")
                .value(String.valueOf(activeCount))
                .change(0)
                .changeType("increase")
                .period("Last 7 days")
                .build());

        // Card 3: Avg Score
        cards.add(StatCardDto.builder()
                .title("Average Score")
                .value(avgScore + "%")
                .change(0)
                .changeType("increase")
                .period("All Quizzes")
                .build());

        return cards;
    }

    private ChartDataDto buildEngagementChart(String username) {
        // 1. Calculate the date range (Last 7 Days)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(6).withHour(0).withMinute(0);

        // 2. Fetch REAL data from Database
        // This gets every quiz result submitted by your students in the last week
        List<QResult> recentResults = qResultRepo.findRecentResultsByInstructor(username, sevenDaysAgo);

        // 3. Prepare buckets for the last 7 days (Map<DateString, Count>)
        Map<LocalDate, Long> countsByDate = new LinkedHashMap<>();
        List<String> labels = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Initialize map with 0 for all 7 days (so days with no quizzes still show up)
        for (int i = 6; i >= 0; i--) {
            LocalDate date = now.minusDays(i).toLocalDate();
            countsByDate.put(date, 0L);
            // Label format: "Mon", "Tue"
            labels.add(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.US));
        }

        // 4. Fill buckets with actual counts
        for (QResult result : recentResults) {
            if (result.getSubmitDate() == null) {
                continue; // Skip active quizzes
            }
            LocalDate submitDate = result.getSubmitDate().toLocalDate();
            // Increment count if the date is in our range
            if (countsByDate.containsKey(submitDate)) {
                countsByDate.put(submitDate, countsByDate.get(submitDate) + 1);
            }
        }

        // 5. Extract just the numbers for the chart
        List<Number> data = new ArrayList<>(countsByDate.values());

        return ChartDataDto.builder()
                .labels(labels)
                .datasets(List.of(
                        ChartDataDto.DatasetDto.builder()
                                .label("Quiz Completions")
                                .data(data) // Now contains REAL counts like [0, 2, 5, 1, 0...]
                                .build()
                ))
                .build();
    }
    private ChartDataDto buildPerformanceBarChart(List<QResult> results) {
        // Group by Quiz Title and get Avg Score
        Map<String, DoubleSummaryStatistics> stats = results.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getQuiz().getTitle(),
                        Collectors.summarizingDouble(r -> (r.getTotalMarks()>0)?(r.getScoreObtained().doubleValue()/r.getTotalMarks())*100:0)
                ));

        List<String> labels = new ArrayList<>(stats.keySet());
        List<Number> data = new ArrayList<>();
        for(String label : labels) {
            data.add(Math.round(stats.get(label).getAverage()));
        }

        return ChartDataDto.builder()
                .labels(labels)
                .datasets(List.of(ChartDataDto.DatasetDto.builder().label("Avg Score").data(data).build()))
                .build();
    }

    private ChartDataDto buildCompletionPieChart(List<QResult> results,String username) {
        // In a real app, compare 'Total Assignments' vs 'Total Results'
        // Here we just map results count as "Completed"
        long totalStudents = assignmentRepo.countStudentsByInstructor(username);
        long totalAssignments = totalStudents * quizRepo.countByInstructor(username);
        long completed = results.size();
        long pending = Math.max(0, totalAssignments - completed);
// Use 'pending' instead of 20

        return ChartDataDto.builder()
                .labels(List.of("Completed", "Pending")) // Pending hard to calc without total expected
                .datasets(List.of(ChartDataDto.DatasetDto.builder().data(List.of(results.size(), pending)).build()))
                .build();
    }

    private ChartDataDto buildDistributionChart(List<StudentPerformanceDto> students) {
        long excellent = students.stream().filter(s -> s.getAverageScore() >= 90).count();
        long good = students.stream().filter(s -> s.getAverageScore() >= 75 && s.getAverageScore() < 90).count();
        long average = students.stream().filter(s -> s.getAverageScore() >= 60 && s.getAverageScore() < 75).count();
        long poor = students.stream().filter(s -> s.getAverageScore() < 60).count();

        return ChartDataDto.builder()
                .labels(List.of("90-100%", "75-89%", "60-74%", "Below 60%"))
                .datasets(List.of(
                        ChartDataDto.DatasetDto.builder()
                                .data(List.of(excellent, good, average, poor))
                                .build()
                ))
                .build();
    }
}