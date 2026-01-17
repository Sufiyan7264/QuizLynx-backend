package com.projectJava.quizApp.service;

import com.projectJava.quizApp.DTO.*;
import com.projectJava.quizApp.model.*;
import com.projectJava.quizApp.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentDashboardService {

    @Autowired private UserRepo userRepo; // CustomerRepo
    @Autowired private AssignmentRepo assignmentRepo; // InstructorStudentAssignment
    @Autowired private QuizRepo quizRepo;
    @Autowired private QResultRepo qResultRepo;
    @Autowired private AnnouncementRepo announcementRepo;

    public StudentDashboardDto getDashboardData(String username) {
        Customer student = userRepo.findByUsername(username).orElseThrow();
        Long studentId = student.getId();

        // 1. Get Batches/Instructors the student is enrolled in
        List<InstructorStudentAssignment> enrollments = assignmentRepo.findByStudentId(studentId);
        List<Long> batchIds = enrollments.stream().map(e -> e.getBatch().getId()).toList();

        // 2. Get All Quizzes for these batches
        List<Quiz> allQuizzes = quizRepo.findActiveQuizzesForBatches(batchIds); // Reusing method from previous step

        // 3. Get All My Results
        List<QResult> myResults = qResultRepo.findByStudentId(studentId);

        // --- DATA PROCESSING ---

        // A. Process Quizzes (Pending vs Completed)
        List<QuizSummaryItemDto> quizList = new ArrayList<>();
        int pendingCount = 0;
        int completedCount = 0;

        for (Quiz quiz : allQuizzes) {
            // Check if I have a result for this quiz
            Optional<QResult> result = myResults.stream()
                    .filter(r -> r.getQuiz().getId().equals(quiz.getId()))
                    .findFirst();

            QuizSummaryItemDto item = QuizSummaryItemDto.builder()
                    .id(quiz.getId().toString())
                    .title(quiz.getTitle())
                    .instructorName(quiz.getBatch().getInstructor().getDisplayName()) // Or user.username
                    .subject(quiz.getSubject())
                    .dueDate(quiz.getEndDate() != null ? quiz.getEndDate().toLocalDate() : null)
                    .maxScore(String.valueOf(quiz.getTotalMarks()))
                    .build();

            if (result.isPresent()) {
                item.setStatus("Completed");
                item.setScore(String.valueOf(result.get().getScoreObtained()));
                completedCount++;
            } else {
                // Check Overdue
                if (quiz.getEndDate() != null && LocalDateTime.now().isAfter(quiz.getEndDate())) {
                    item.setStatus("Overdue");
                } else {
                    item.setStatus("Pending");
                    pendingCount++;
                }
                item.setScore("-");
            }
            quizList.add(item);
        }

        // B. Calculate Overall Average
        double overallAvg = myResults.stream()
                .mapToDouble(r -> (r.getTotalMarks() > 0) ? (r.getScoreObtained().doubleValue() / r.getTotalMarks()) * 100 : 0)
                .average().orElse(0.0);

        // C. Build Instructors List
        List<InstructorSummaryDto> instructorList = enrollments.stream().map(enrollment -> {
            InstructorProfile instructor = enrollment.getBatch().getInstructor();

            // Filter quizzes/results specific to this instructor
            List<Quiz> instructorsQuizzes = allQuizzes.stream()
                    .filter(q -> q.getBatch().getInstructor().getId().equals(instructor.getId()))
                    .toList();

            List<QResult> instructorResults = myResults.stream()
                    .filter(r -> r.getQuiz().getBatch().getInstructor().getId().equals(instructor.getId()))
                    .toList();

            double myAvgForInstructor = instructorResults.stream()
                    .mapToDouble(r -> (r.getTotalMarks()>0)?(r.getScoreObtained().doubleValue()/r.getTotalMarks())*100:0)
                    .average().orElse(0.0);

            return InstructorSummaryDto.builder()
                    .id(instructor.getId().toString())
                    .name(instructor.getDisplayName())
                    .email(instructor.getCustomer().getEmail())
//                    .subject(instructor.getSubjects() != null ? instructor.getSubjects().get(0) : "General") // Pick first subject
                    .totalQuizzes(instructorsQuizzes.size())
                    .averageScore(Math.round(myAvgForInstructor))
                    .lastActive("Today") // Placeholder
                    .build();
        }).collect(Collectors.toList());

        // D. Fetch Announcements
        List<Announcement> announcements = announcementRepo.findByBatchIdIn(batchIds);
        List<AnnouncementDto> announcementDtos = announcements.stream().map(a -> AnnouncementDto.builder()
                .id(a.getId().toString())
                .title(a.getTitle())
                .message(a.getMessage())
                .instructorName(a.getInstructor().getDisplayName())
                .priority(a.getPriority())
                .date(a.getDate().toString())
                .build()).collect(Collectors.toList());

        // E. Extract Feedback
        List<FeedbackDto> feedbacks = myResults.stream()
                .filter(r -> r.getInstructorFeedback() != null && !r.getInstructorFeedback().isEmpty())
                .map(r -> FeedbackDto.builder()
                        .id(r.getId().toString())
                        .quizTitle(r.getQuiz().getTitle())
                        .instructorName(r.getQuiz().getBatch().getInstructor().getDisplayName())
                        .feedback(r.getInstructorFeedback())
                        .date(r.getSubmitDate().toString())
                        .rating(4) // Placeholder or add rating to DB
                        .build())
                .limit(5)
                .collect(Collectors.toList());

        // F. Build Charts
        // 1. Performance (My Score vs Class Average - Mocking Class Avg for now)
        ChartDataDto performanceChart = ChartDataDto.builder()
                .labels(List.of("My Score", "Class Average"))
                .datasets(List.of(
                        ChartDataDto.DatasetDto.builder().data(List.of(Math.round(overallAvg), 78)).build() // 78 is mocked class avg
                )).build();

        // 2. Quiz By Instructor
        List<String> instLabels = new ArrayList<>();
        List<Number> instData = new ArrayList<>();
        instructorList.forEach(i -> {
            instLabels.add(i.getName());
            instData.add(i.getTotalQuizzes());
        });

        ChartDataDto completionChart = ChartDataDto.builder()
                .labels(instLabels)
                .datasets(List.of(ChartDataDto.DatasetDto.builder().data(instData).build())).build();


        // G. Assemble Final DTO
        return StudentDashboardDto.builder()
                .stats(List.of(
                        StatCardDto.builder().title("My Instructors").value(String.valueOf(instructorList.size())).changeType("increase").build(),
                        StatCardDto.builder().title("Instructor Quizzes").value(String.valueOf(allQuizzes.size())).changeType("increase").build(),
                        StatCardDto.builder().title("Average Score").value(Math.round(overallAvg) + "%").changeType("increase").build(),
                        StatCardDto.builder().title("Pending Quizzes").value(String.valueOf(pendingCount)).changeType("decrease").build()
                ))
                .instructors(instructorList)
                .instructorQuizzes(quizList)
                .instructorAnnouncements(announcementDtos)
                .instructorFeedback(feedbacks)
                .performanceComparison(performanceChart)
                .quizCompletion(completionChart)
                .build();
    }
}