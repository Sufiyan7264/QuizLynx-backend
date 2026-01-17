package com.projectJava.quizApp.service;

import com.projectJava.quizApp.DTO.CreateInstructorProfileRequest;
import com.projectJava.quizApp.model.Batch; // Import Batch
import com.projectJava.quizApp.model.Customer;
import com.projectJava.quizApp.model.InstructorProfile;
import com.projectJava.quizApp.model.InstructorStudentAssignment;
import com.projectJava.quizApp.repo.AssignmentRepo;
import com.projectJava.quizApp.repo.BatchRepo; // You need BatchRepo
import com.projectJava.quizApp.repo.InstructorProfileRepo;
import com.projectJava.quizApp.repo.UserRepo;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InstructorService {
    @Autowired
    private UserRepo customer;
    @Autowired
    private AssignmentRepo assignmentRepo;
    @Autowired
    private InstructorProfileRepo profileRepo;
    @Autowired
    private BatchRepo batchRepo; // Add this!

    public Optional<InstructorProfile> findByUserId(Long userId){
        return profileRepo.findByCustomer_Id(userId);
    }

    // You might keep this to find by code if you still use instructor codes
    // But usually, you search by Batch Code now
    public Optional<InstructorProfile> findByCode(String code){
        return profileRepo.findByInstructorCode(code);
    }

    @Transactional
    public InstructorProfile createProfile(Long userId, CreateInstructorProfileRequest request){
        Customer user = customer.findById(userId).orElseThrow(()->new RuntimeException("User Not Found"));
        if (profileRepo.findByCustomer_Id(userId).isPresent()){
            throw new RuntimeException("Profile Already Exist");
        }
        InstructorProfile instructorProfile = new InstructorProfile();
        instructorProfile.setCustomer(user);
        instructorProfile.setDisplayName(request.getDisplayName());
        instructorProfile.setBio(request.getBio());
        instructorProfile.setSubjects(request.getSubjects().toArray(new String[0]));
        instructorProfile.setAvatarUrl(request.getAvatarUrl());
        instructorProfile.setInstructorCode(generateUniqueCode());
        return profileRepo.save(instructorProfile);
    }

    // --- ❌ REMOVED: bindStudent (Direct Binding) ---
    // --- ✅ ADDED: joinBatch (Batch Binding) ---

    @Transactional
    public void joinBatch(Long studentId, String batchCode){
        // 1. Find the Batch (Class)
        Batch batch = batchRepo.findByBatchCode(batchCode)
                .orElseThrow(() -> new RuntimeException("Invalid Batch Code"));

        // 2. Check if student is already in this specific Batch
        // (Make sure you added existsByStudentIdAndBatchId to your Repo)
        if(assignmentRepo.existsByStudentIdAndBatchId(studentId, batch.getId())){
            throw new RuntimeException("You are already in this class");
        }

        Customer student = customer.findById(studentId)
                .orElseThrow(()->new RuntimeException("Student not found"));

        // 3. Create the assignment
        InstructorStudentAssignment assignments = new InstructorStudentAssignment();
        assignments.setStudent(student);

        // AUTO-LINK: We get the instructor FROM the batch
        assignments.setInstructorProfile(batch.getInstructor());

        // Link the specific batch
        assignments.setBatch(batch);

        assignments.setActive(true);
        assignments.setNotes("Joined Class: " + batch.getBatchName());

        assignmentRepo.save(assignments);
    }

    @Transactional
    public InstructorProfile updateProfile(Long userId, CreateInstructorProfileRequest req) {
        InstructorProfile profile = profileRepo.findByCustomer_Id(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        profile.setDisplayName(req.getDisplayName());
        profile.setBio(req.getBio());
        profile.setAvatarUrl(req.getAvatarUrl());

        if (req.getSubjects() != null) {
            profile.setSubjects(req.getSubjects().toArray(new String[0]));
        }

        return profileRepo.save(profile);
    }

    @Transactional
    public void removeStudent(Long instructorUserId, Long studentId) {
        InstructorProfile profile = profileRepo.findByCustomer_Id(instructorUserId)
                .orElseThrow(() -> new RuntimeException("Instructor profile not found"));

        // Note: You might want to remove them from a specific batch,
        // but removing by instructor ID works to remove them generally from that teacher.
        InstructorStudentAssignment assignment = assignmentRepo.findByStudentIdAndInstructorProfileIdAndActiveTrue(studentId, profile.getId())
                .orElseThrow(() -> new RuntimeException("Student is not bound to you"));

        assignment.setActive(false);
        assignmentRepo.save(assignment);
    }

    public List<InstructorStudentAssignment> getStudents(Long instructorProfileId){
        return assignmentRepo.findActiveByInstructor(instructorProfileId);
    }
    // Add this to InstructorService.java if missing
    public Optional<InstructorProfile> findById(Long id){
        return profileRepo.findById(id);
    }

    private String generateUniqueCode(){
        return RandomStringUtils.randomAlphanumeric(6).toUpperCase();
    }
}