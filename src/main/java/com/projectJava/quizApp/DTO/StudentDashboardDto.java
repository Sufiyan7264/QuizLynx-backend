package com.projectJava.quizApp.DTO;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class StudentDashboardDto {
    private List<StatCardDto> stats;
    private List<InstructorSummaryDto> instructors;
    private List<QuizSummaryItemDto> instructorQuizzes;
    private List<FeedbackDto> instructorFeedback;
    private List<AnnouncementDto> instructorAnnouncements;

    // Charts
    private ChartDataDto performanceComparison;
    private ChartDataDto quizCompletion;
}