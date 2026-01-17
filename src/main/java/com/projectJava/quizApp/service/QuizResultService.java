package com.projectJava.quizApp.service;

import com.projectJava.quizApp.DTO.QuestionReviewDto;
import com.projectJava.quizApp.DTO.QuizResultDto; // or QuizResultsShowDto if you renamed it
import com.projectJava.quizApp.model.QResult;
import com.projectJava.quizApp.model.Question;
import com.projectJava.quizApp.model.Quiz;
import com.projectJava.quizApp.model.SubmittedAnswer;
import com.projectJava.quizApp.repo.QResultRepo;
import com.projectJava.quizApp.repo.QuizRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration; // Only if you track start time
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizResultService {

    @Autowired
    private QResultRepo qResultRepo; // Changed from QuizSubmissionRepo

    // 1. Logic for "loadResult" (By Result ID)
    public QuizResultDto getResultBySubmissionId(Long resultId) {
        QResult result = qResultRepo.findById(resultId)
                .orElseThrow(() -> new RuntimeException("Result not found"));

        return mapToDTO(result);
    }

    // 2. Logic for "loadResultByQuizId" (Finds latest attempt)
    public QuizResultDto getLatestResultForStudent(Long quizId, String username) {
        // 1. Fetch all results (or use a specific Repo method if you added one)
        List<QResult> results = qResultRepo.findAll();

        // 2. Filter for BOTH Quiz ID and Username
        List<QResult> studentResults = results.stream()
                .filter(r -> r.getQuiz().getId().equals(quizId))
                .filter(r -> r.getStudent().getUsername().equals(username)) // <--- MATCH USERNAME
                .collect(Collectors.toList());

        if (studentResults.isEmpty()) {
            // This is likely the error you were seeing
            throw new RuntimeException("No attempts found for this quiz.");
        }

        // 3. Get the latest one
        QResult latestResult = studentResults.stream()
                .filter(r -> r.getSubmitDate() != null) // <--- THE CRITICAL FIX
                .max(Comparator.comparing(QResult::getSubmitDate))
                .orElseThrow(() -> new RuntimeException("No completed attempts found for this quiz."));

        return mapToDTO(latestResult);
    }
    public Page<QuizResultDto> getMyAttempts(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page,size);
        Page<QResult> attempts = qResultRepo.findByStudent_UsernameAndSubmitDateIsNotNullOrderBySubmitDateDesc(username,pageable);

        return attempts.map(this::mapToSummaryDTO);
    }

    private QuizResultDto mapToSummaryDTO(QResult result) {
        QuizResultDto dto = new QuizResultDto();
        dto.setId(result.getId());
        dto.setTopic(result.getQuiz().getSubject());
        dto.setQuizId(result.getQuiz().getId());
        dto.setQuizTitle(result.getQuiz().getTitle());
        dto.setSubmittedAt(result.getSubmitDate());
        dto.setStartedAt(result.getStartTime());
        dto.setScore(result.getScoreObtained().doubleValue());
        dto.setTotalMarks(result.getTotalMarks().doubleValue());

        double percentage = 0.0;
        if (result.getTotalMarks() > 0) {
            percentage = (result.getScoreObtained().doubleValue() / result.getTotalMarks().doubleValue()) * 100.0;
        }
        dto.setPercentage(Math.round(percentage * 10.0) / 10.0);

        int passingMarks = result.getQuiz().getPassingMarks() != null ? result.getQuiz().getPassingMarks() : 0;
        dto.setPassed(result.getScoreObtained() >= passingMarks);

        return dto;
    }

    private QuizResultDto mapToDTO(QResult result) {
        QuizResultDto dto = new QuizResultDto();

        dto.setId(result.getId());
        dto.setQuizTitle(result.getQuiz().getTitle());
        dto.setQuizId(result.getQuiz().getId());
        dto.setSubmittedAt(result.getSubmitDate());
        dto.setStartedAt(result.getStartTime());
        dto.setTimeTaken(0L);
        dto.setScore(result.getScoreObtained().doubleValue());
        dto.setTotalMarks(result.getTotalMarks().doubleValue());

        double percentage = 0.0;
        if (result.getTotalMarks() > 0) {
            percentage = (result.getScoreObtained().doubleValue() / result.getTotalMarks().doubleValue()) * 100.0;
        }
        dto.setPercentage(Math.round(percentage * 10.0) / 10.0);

        int passingMarks = result.getQuiz().getPassingMarks() != null ? result.getQuiz().getPassingMarks() : 0;
        dto.setPassed(result.getScoreObtained() >= passingMarks);

        List<SubmittedAnswer> answers = result.getSubmittedAnswers();
        if (answers == null) {
            answers = Collections.emptyList();
        }

        List<QuestionReviewDto> reviews = answers.stream()
                .map(answer -> {
                    QuestionReviewDto review = new QuestionReviewDto();
                    Question q = answer.getQuestion();

                    review.setQuestionId(q.getId().toString());
                    review.setQuestionText(q.getQuestionTitle());
                    review.setOption1(q.getOption1());
                    review.setOption2(q.getOption2());
                    review.setOption3(q.getOption3());
                    review.setOption4(q.getOption4());
                    review.setSelectedAnswer(answer.getSelectedResponse());
                    review.setCorrectAnswer(q.getRightAnswer());

                    boolean isCorrect = answer.getSelectedResponse() != null &&
                            answer.getSelectedResponse().trim().equalsIgnoreCase(q.getRightAnswer().trim());

                    review.setCorrect(isCorrect);
                    double maxMarks = (q.getMarks() != null) ? q.getMarks() : 1.0;
                    review.setMarks(maxMarks);
                    review.setEarnedMarks(isCorrect ? maxMarks : 0.0);

                    return review;
                })
                .collect(Collectors.toList());

        dto.setQuestionReviews(reviews);

        return dto;
    }

    public List<QuizResultDto> getMistakeResults(String name) {
        List<QResult> mistakeResult = qResultRepo.findByStudent_UsernameOrderBySubmitDateDesc(name);
        return mistakeResult.stream()
                .filter(r->!r.getTotalMarks().equals(r.getScoreObtained()))
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }
}