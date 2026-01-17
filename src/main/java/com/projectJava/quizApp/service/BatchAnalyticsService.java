package com.projectJava.quizApp.service;

import com.projectJava.quizApp.DTO.BatchQuizResultDto;
import com.projectJava.quizApp.DTO.StudentResultItemDto;
import com.projectJava.quizApp.model.*;
import com.projectJava.quizApp.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BatchAnalyticsService {

    @Autowired private QuizRepo quizRepo;
    @Autowired private QResultRepo qResultRepo;
    @Autowired private AssignmentRepo assignmentRepo;
    @Autowired private UserRepo customerRepo; // or CustomerRepo

    public BatchQuizResultDto getBatchQuizResults(Long batchId, Long quizId, String instructorUsername) {

        // 1. Validate Quiz & Batch
        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        if (!quiz.getBatch().getId().equals(batchId)) {
            throw new RuntimeException("Quiz does not belong to the selected batch");
        }

        // 2. Validate Instructor Ownership
        Customer instructor = customerRepo.findByUsername(instructorUsername)
                .orElseThrow(() -> new RuntimeException("Instructor not found"));

        if (!quiz.getBatch().getInstructor().getCustomer().getId().equals(instructor.getId())) {
            throw new RuntimeException("Unauthorized: You do not own this batch");
        }

        // 3. Prepare DTO
        BatchQuizResultDto response = new BatchQuizResultDto();
        response.setQuizTitle(quiz.getTitle());

        // 4. Get Total Enrolled Students (For "Total Students" card)
        long totalEnrolled = assignmentRepo.countByBatchId(batchId);
        response.setTotalStudents(totalEnrolled);

        // 5. Fetch Actual Results
        List<QResult> submissions = qResultRepo.findByQuizId(quizId);
        response.setStudentsCompleted(submissions.size());

        if (submissions.isEmpty()) {
            response.setAveragePercentage(0.0);
            response.setStudentResults(new ArrayList<>());
            return response;
        }

        // 6. Map to StudentResultItemDto & Calculate Class Average
        double totalPercentageSum = 0;
        List<StudentResultItemDto> resultList = new ArrayList<>();

        for (QResult submission : submissions) {
            StudentResultItemDto item = new StudentResultItemDto();

            // Student Details
            Customer student = submission.getStudent();
            item.setStudentId(student.getId());
            item.setStudentName(student.getUsername()); // Or student.getFullName()
            item.setStudentEmail(student.getEmail());

            // Score Details
            double score = submission.getScoreObtained().doubleValue();
            double total = submission.getTotalMarks().doubleValue();
            item.setScore(score);
            item.setTotalMarks(total);

            // Percentage
            double pct = (total > 0) ? (score / total) * 100 : 0;
            item.setPercentage(pct);
            totalPercentageSum += pct;

            // Dates & Time
            item.setSubmittedAt(submission.getSubmitDate());

            // Calculate Duration (Assuming you added startTime to QResult entity, otherwise 0)
            // If QResult doesn't have startTime, remove this logic or return 0
            /* if (submission.getStartTime() != null && submission.getSubmitDate() != null) {
                long seconds = Duration.between(submission.getStartTime(), submission.getSubmitDate()).getSeconds();
                item.setTimeTaken(seconds);
            } else {
                item.setTimeTaken(0L);
            }
            */
            item.setTimeTaken(0L); // Placeholder until startTime is added to Entity

            // Pass/Fail
            boolean passed = score >= (quiz.getPassingMarks() != null ? quiz.getPassingMarks() : 0);
            item.setPassed(passed);

            resultList.add(item);
        }

        response.setStudentResults(resultList);

        // 7. Finalize Average
        double classAvg = totalPercentageSum / submissions.size();
        response.setAveragePercentage(Math.round(classAvg * 10.0) / 10.0); // Round 1 decimal

        return response;
    }
}