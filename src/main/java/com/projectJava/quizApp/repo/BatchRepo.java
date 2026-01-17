package com.projectJava.quizApp.repo;

import com.projectJava.quizApp.model.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BatchRepo extends JpaRepository<Batch, Long> {
    // Find all batches for a specific instructor
    List<Batch> findByInstructorId(Long instructorId);
    boolean existsByInstructorIdAndBatchName(Long instructorId, String batchName);
    // Find batch by its unique code (for student joining)
    Optional<Batch> findByBatchCode(String batchCode);
}