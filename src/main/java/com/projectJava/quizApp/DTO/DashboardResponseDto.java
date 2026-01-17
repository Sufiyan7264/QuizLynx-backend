package com.projectJava.quizApp.DTO;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardResponseDto {
    // 1. Stat Cards
    private List<StatCardDto> stats;

    // 2. Quick Stats Object
    private QuickStatsDto quickStats;

    // 3. Charts Data
    private ChartDataDto studentEngagement; // Line Chart
    private ChartDataDto studentPerformance; // Bar Chart
    private ChartDataDto quizCompletion;     // Pie Chart
    private ChartDataDto performanceDistribution; // Pie Chart (Summary)

    // 4. Tables
    private List<StudentPerformanceDto> topStudents;
    private List<StudentPerformanceDto> studentsNeedingAttention;
}