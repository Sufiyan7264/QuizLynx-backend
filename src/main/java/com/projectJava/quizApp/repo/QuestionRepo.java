package com.projectJava.quizApp.repo;

import com.projectJava.quizApp.model.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepo extends JpaRepository<Question, Long> {

    // NEW: Find questions belonging to a specific Quiz
    List<Question> findByQuizId(Long quizId);
    // You can keep your old methods if you want, but you might not use them anymore
//    Page<Question> findByCategory(String category, Pageable pageable);
}