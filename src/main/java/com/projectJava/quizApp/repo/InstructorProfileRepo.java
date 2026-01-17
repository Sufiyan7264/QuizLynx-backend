package com.projectJava.quizApp.repo;

import com.projectJava.quizApp.model.InstructorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface InstructorProfileRepo extends JpaRepository<InstructorProfile, Long> {
    Optional<InstructorProfile> findByCustomer_Id(Long userId);
    Optional<InstructorProfile> findByInstructorCode(String code);
}
