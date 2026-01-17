package com.projectJava.quizApp.repo;

import com.projectJava.quizApp.model.Customer;
import com.projectJava.quizApp.model.InstructorStudentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepo extends JpaRepository<InstructorStudentAssignment, Long> {
    @Query("SELECT a FROM InstructorStudentAssignment a WHERE a.instructorProfile.id = :profileId AND a.active = true")
    List<InstructorStudentAssignment> findActiveByInstructor(@Param("profileId") Long profileId);
    long countByBatchIdAndActiveTrue(Long batchId);
    void deleteByBatchId(Long batchId);
    boolean existsByStudentIdAndBatchIdAndActiveTrue(Long studentId, Long batchId);
    List<InstructorStudentAssignment> findByBatchIdAndActiveTrue(Long batchId);
    boolean existsByStudentIdAndBatchId(Long studentId, Long batchId);
    List<InstructorStudentAssignment> findByStudentId(Long studentId);
    // Inside AssignmentRepository interface
    List<InstructorStudentAssignment> findByStudentIdAndActiveTrue(Long studentId);
    List<InstructorStudentAssignment> findByBatchId(Long batchId);
    // To calculate "Total Students" in the summary card
    long countByBatchId(Long batchId);
    Optional<InstructorStudentAssignment> findByStudentIdAndInstructorProfileIdAndActiveTrue(Long studentId, Long instructorProfileId);

    // Count total students linked to this instructor
    @Query("SELECT COUNT(DISTINCT a.student.id) FROM InstructorStudentAssignment a " +
            "WHERE a.batch.instructor.customer.username = :username")
    long countStudentsByInstructor(@Param("username") String username);

    // Get all students for this instructor (to calculate averages)
    @Query("SELECT DISTINCT a.student FROM InstructorStudentAssignment a " +
            "WHERE a.batch.instructor.customer.username = :username")
    List<Customer> findStudentsByInstructor(@Param("username") String username);

    Optional<InstructorStudentAssignment> findByStudentIdAndBatchId(Long studentId, Long batchId);
}
