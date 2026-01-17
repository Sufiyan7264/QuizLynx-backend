package com.projectJava.quizApp.service;

import com.projectJava.quizApp.DTO.InstructorDTO;
import com.projectJava.quizApp.model.Batch;
import com.projectJava.quizApp.model.Customer;
import com.projectJava.quizApp.model.InstructorProfile;
import com.projectJava.quizApp.model.InstructorStudentAssignment;
import com.projectJava.quizApp.repo.AssignmentRepo;
import com.projectJava.quizApp.repo.BatchRepo;
import com.projectJava.quizApp.repo.InstructorProfileRepo;
import com.projectJava.quizApp.repo.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class StudentService {

    @Autowired
    private AssignmentRepo assignmentRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private BatchRepo batchRepo;

    @Autowired
    private InstructorProfileRepo instructorProfileRepo;

    public List<InstructorDTO> getMyInstructor(Long studentId){
        List<InstructorStudentAssignment> assignments =assignmentRepo.findByStudentIdAndActiveTrue(studentId);
        return assignments.stream()
                .map(a->InstructorDTO.from(a.getInstructorProfile()))
                .toList();
    }

    @Transactional
    public void joinBatch(Long studentId, String code) {
        // 1. Find Batch by Code (Instead of Instructor Profile)
        Batch batch = batchRepo.findByBatchCode(code)
                .orElseThrow(() -> new RuntimeException("Invalid Class Code"));

        // 2. Check if already in this batch
        // (You need to add existsByStudentIdAndBatchId to AssignmentRepo)
        if (assignmentRepo.existsByStudentIdAndBatchId(studentId, batch.getId())) {
            throw new RuntimeException("You have already joined this class.");
        }

        Customer student = userRepo.findById(studentId).orElseThrow();

        // 3. Create Assignment linked to BOTH Instructor and Batch
        InstructorStudentAssignment assignment = new InstructorStudentAssignment();
        assignment.setStudent(student);
        assignment.setInstructorProfile(batch.getInstructor()); // Auto-link to the teacher
        assignment.setBatch(batch); // Link to "Class 8"
        assignment.setActive(true);
        assignment.setNotes("Joined Class: " + batch.getBatchName());

        assignmentRepo.save(assignment);
    }
}
