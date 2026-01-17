package com.projectJava.quizApp.service;

import com.projectJava.quizApp.DTO.OpenTdbResponse;
import com.projectJava.quizApp.enums.QuizStatus;
import com.projectJava.quizApp.model.*;
import com.projectJava.quizApp.repo.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.HtmlUtils; // To unescape HTML entities

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class QuizGeneratorService {

    @Autowired private QuestionRepo questionRepo;
    @Autowired private QuizRepo quizRepo;
    @Autowired private BatchRepo batchRepo; // Optional: If you need a dummy batch for users

    private final String API_URL = "https://opentdb.com/api.php?amount=10&type=multiple";

    // Standard Configuration
    private static final int Q_COUNT = 10;
    private static final int TIME_PER_Q_MIN = 1;

    @Transactional
    public Long generateInstantQuiz(String category, String difficulty) {

        // 1. Map UI Categories to OpenTDB Category IDs
        // (See OpenTDB docs for IDs: 18=Computers, 19=Math, 23=History, etc.)
        String title = difficulty + " " + category + " Challenge";
        Optional<Quiz> existingQuiz = quizRepo.findTopByTitleAndBatchIsNull(title);
        if (existingQuiz.isPresent()) {
            System.out.println("Returning existing quiz: " + title);
            return existingQuiz.get().getId(); // <--- STOP HERE and return existing ID
        }
        int apiCategoryId = getCategoryId(category);

        // 2. Try to fetch enough unused questions from LOCAL DB first
        // (You would need a complex query here, or just simple fetch for now)
        // For this example, we assume we fetch fresh ones or rely on the external API.

        // 3. Call External API
        String url = API_URL + "&category=" + apiCategoryId + "&difficulty=" + difficulty.toLowerCase();
        RestTemplate restTemplate = new RestTemplate();
        OpenTdbResponse response = restTemplate.getForObject(url, OpenTdbResponse.class);

        if (response == null || response.getResults().isEmpty()) {
            throw new RuntimeException("Could not generate quiz at this time.");
        }

        // 4. Create the Quiz Container
        Quiz quiz = new Quiz();
        quiz.setTitle(difficulty + " " + category + " Challenge");
        quiz.setDescription("Auto-generated quiz to test your skills in " + category);
        quiz.setSubject(category);
        quiz.setStatus(QuizStatus.PUBLISHED);
        quiz.setDifficulty(difficulty);
        quiz.setStartDate(LocalDateTime.now());
        quiz.setEndDate(LocalDateTime.now().plusYears(1)); // Valid for a year
        quiz.setTimerInMin(Q_COUNT * TIME_PER_Q_MIN); // 10 mins

        // Calculate Marks
        int marksPerQ = getMarksForDifficulty(difficulty);
        quiz.setTotalMarks(Q_COUNT * marksPerQ);
        quiz.setPassingMarks((int) (quiz.getTotalMarks() * 0.6)); // 60% passing

        // Assign to a generic "Global" batch or leave null if your DB allows
        // quiz.setBatch(globalUserBatch);

        quiz = quizRepo.save(quiz);

        // 5. Convert External Questions to Internal Questions
        List<Question> savedQuestions = new ArrayList<>();

        for (OpenTdbResponse.ExternalQuestion extQ : response.getResults()) {
            Question q = new Question();
            q.setQuestionTitle(HtmlUtils.htmlUnescape(extQ.getQuestion())); // Fix &quot; etc
            q.setQuiz(quiz);
            q.setMarks(marksPerQ);

            // Randomize Options
            List<String> options = new ArrayList<>(extQ.getIncorrect_answers());
            options.add(extQ.getCorrect_answer());
            Collections.shuffle(options); // Randomize order

            q.setOption1(HtmlUtils.htmlUnescape(options.get(0)));
            q.setOption2(HtmlUtils.htmlUnescape(options.get(1)));
            q.setOption3(HtmlUtils.htmlUnescape(options.get(2)));
            q.setOption4(HtmlUtils.htmlUnescape(options.get(3)));
            q.setRightAnswer(HtmlUtils.htmlUnescape(extQ.getCorrect_answer()));

            savedQuestions.add(q);
        }

        questionRepo.saveAll(savedQuestions);

        return quiz.getId(); // Return ID so frontend can navigate to /quiz/attempt/{id}
    }


    private int getCategoryId(String name) {
        String lower = name.toLowerCase();

        // Science & Nature
        if (lower.contains("bio") || lower.contains("nature") || lower.contains("science")) return 17;

        // Computers
        if (lower.contains("java") || lower.contains("python") || lower.contains("computer") || lower.contains("code") || lower.contains("programming")) return 18;

        // Math
        if (lower.contains("math") || lower.contains("algebra") || lower.contains("calc")) return 19;

        // History
        if (lower.contains("history") || lower.contains("war") || lower.contains("past")) return 23;

        // Geography
        if (lower.contains("geo") || lower.contains("earth") || lower.contains("country")) return 22;

        // Arts / Books
        if (lower.contains("art") || lower.contains("book") || lower.contains("novel")) return 10;

        // General fallback
        return 9; // General Knowledge
    }
    private int getMarksForDifficulty(String difficulty) {
        switch (difficulty.toLowerCase()) {
            case "hard": return 3;
            case "medium": return 2;
            default: return 1;
        }
    }
}