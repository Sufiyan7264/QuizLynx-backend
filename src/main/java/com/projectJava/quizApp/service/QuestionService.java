package com.projectJava.quizApp.service;

import com.projectJava.quizApp.model.Question;
import com.projectJava.quizApp.model.Quiz;
import com.projectJava.quizApp.repo.QuestionRepo;
import com.projectJava.quizApp.repo.QuizRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {
    @Autowired
    private QuestionRepo questionRepo;

    @Autowired
    private QuizRepo quizRepo; // You need this now

    // CHANGED: We now need the quizId to know where to put the question
    public void addQuestion(Long quizId, Question question) {
        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        // Link them
        question.setQuiz(quiz);

        questionRepo.save(question);
    }

    // CHANGED: Get questions by Quiz ID (not all questions)
    public List<Question> getQuestionsByQuizId(Long quizId) {
        return questionRepo.findByQuizId(quizId);
    }

    public void updateQuestion(Long id, Question updated) {
        // 1. Fetch the existing question from DB (which currently has the Quiz link)
        Question existingQuestion = questionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // 2. Update ONLY the content fields (Title, Options, Answer, Marks)
        existingQuestion.setQuestionTitle(updated.getQuestionTitle());
        existingQuestion.setOption1(updated.getOption1());
        existingQuestion.setOption2(updated.getOption2());
        existingQuestion.setOption3(updated.getOption3());
        existingQuestion.setOption4(updated.getOption4());
        existingQuestion.setRightAnswer(updated.getRightAnswer());
        existingQuestion.setMarks(updated.getMarks());

        // CRITICAL: Do NOT call existingQuestion.setQuiz(...)
        // By not touching it, the existing link to the Quiz remains intact.

        // 3. Save the existing entity
        questionRepo.save(existingQuestion);
    }

    public void deleteQuestion(Long id) {
        questionRepo.deleteById(id);
    }

    public Question getQuestionById(Long id) {
        return questionRepo.findById(id).orElseThrow();
    }
}