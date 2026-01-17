package com.projectJava.quizApp.service;

import com.projectJava.quizApp.DTO.*;
import com.projectJava.quizApp.model.*;
import com.projectJava.quizApp.repo.*;
import com.projectJava.quizApp.service.QuizService;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BatchService {

    @Autowired
    private BatchRepo batchRepo;
    @Autowired
    private InstructorProfileRepo instructorRepo;
    @Autowired
    private QuizRepo quizRepo;
    @Autowired
    private UserRepo customerRepo;
    @Autowired private QResultRepo qResultRepo; // Ensure this is autowired

    @Autowired
    private AssignmentRepo assignmentRepo;

    // Inside BatchService.java

    @Autowired
    private QuizService quizService; // Ensure this is injected

    public StudentBatchDto getBatchDetailsForStudent(Long batchId, Long studentId) {
        // 1. SECURITY: Check if Student is enrolled in this Batch
        // (Ensure this method exists in AssignmentRepo, or use a custom query)
        boolean isEnrolled = assignmentRepo.existsByStudentIdAndBatchIdAndActiveTrue(studentId, batchId);

        if (!isEnrolled) {
            throw new RuntimeException("You are not enrolled in this batch.");
        }

        // 2. Fetch Batch Data
        Batch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        // 3. Fetch Only ACTIVE Quizzes for this batch
        // (Reuse the logic from QuizService to filter by date/status)
        List<QuizDto> validQuizzes = quizService.getQuizzesByBatch(batchId, studentId);

        // 4. Map to Safe DTO
        StudentBatchDto response = new StudentBatchDto();
        response.setId(batch.getId());
        response.setBatchName(batch.getBatchName());
        response.setDescription(batch.getDescription());
        // Safe Instructor Info (Handle potential nulls safely)
        if (batch.getInstructor() != null && batch.getInstructor().getCustomer() != null) {
            response.setInstructorName(batch.getInstructor().getCustomer().getUsername());
        }
//        response.setA(validQuizzes);

        return response;
    }

    // 1. Create a new Batch (Class)
    public BatchResponseDTO createBatch(Long userId, String batchName, String description) {
        // 1. Find the Instructor
        InstructorProfile instructor = instructorRepo.findByCustomer_Id(userId)
                .orElseThrow(() -> new RuntimeException("Instructor not found"));

        // 2. NEW: VALIDATION CHECK
        // "Does this instructor already have a batch named 'Class 8'?"
        boolean exists = batchRepo.existsByInstructorIdAndBatchName(instructor.getId(), batchName);

        if (exists) {
            throw new RuntimeException("You already have a class named '" + batchName + "'. Please choose a different name.");
        }

        // 3. Create and Save (Existing logic)
        Batch batch = new Batch();
        batch.setBatchName(batchName);
        batch.setBatchCode(generateBatchCode(batchName)); // Your helper method
        batch.setDescription(description);
        batch.setInstructor(instructor);

        Batch savedBatch = batchRepo.save(batch);

        // 4. Return DTO
        return new BatchResponseDTO(
                savedBatch.getId(),
                savedBatch.getBatchName(),
                savedBatch.getBatchCode(),
                savedBatch.getDescription(),
                0, // New batch has 0 students
                0
        );
    }
    // Change return type from List<Batch> to List<BatchResponseDto>
    public List<BatchResponseDTO> getMyBatches(Long userId) {

        // 1. Find the Instructor
        InstructorProfile instructor = instructorRepo.findByCustomer_Id(userId)
                .orElseThrow(() -> new RuntimeException("Instructor not found"));

        // 2. Find their Batches
        List<Batch> batches = batchRepo.findByInstructorId(instructor.getId());

        // 3. Map to DTO + Add Student Count
        return batches.stream().map(batch -> {

            // Fetch the count for this specific batch
            long studentCount = assignmentRepo.countByBatchIdAndActiveTrue(batch.getId());
            long quizCount = quizRepo.countByBatchId(batch.getId());

            // Return clean DTO (No sensitive data)
            return new BatchResponseDTO(
                    batch.getId(),
                    batch.getBatchName(),
                    batch.getBatchCode(),
                    batch.getDescription(), // assuming you added this field
                    studentCount,
                    quizCount
            );
        }).collect(Collectors.toList());
    }

    @Transactional
    public void deleteBatch(Long batchId, Long userId, boolean forceDelete) {
        // 1. Find Batch & Verify Owner
        Batch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        if (!batch.getInstructor().getCustomer().getId().equals(userId)) {
            throw new RuntimeException("You do not have permission to delete this class");
        }

        // 2. Check for dependencies
        long studentCount = assignmentRepo.countByBatchIdAndActiveTrue(batchId);
        long quizCount = quizRepo.countByBatchId(batchId);

        // 3. LOGIC: If not empty AND force is false -> Stop and Warn
        if (!forceDelete && (studentCount > 0 || quizCount > 0)) {
            // We throw a specific exception that the controller can catch
            throw new RuntimeException("DependencyWarning: This class contains " + studentCount + " students and " + quizCount + " quizzes.");
        }

        // 4. Execution (Only reaches here if Empty OR forceDelete is TRUE)
        assignmentRepo.deleteByBatchId(batchId); // Delete links
        quizRepo.deleteByBatchId(batchId);       // Delete quizzes
        batchRepo.delete(batch);                 // Delete class
    }

    // Inside BatchService.java

    public BatchResponseDTO updateBatch(Long batchId, Long userId, String newName, String newDescription) {
        // 1. Find Batch & Verify Owner
        Batch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        if (!batch.getInstructor().getCustomer().getId().equals(userId)) {
            throw new RuntimeException("You do not have permission to update this class");
        }

        // 2. Validate Name (Don't allow renaming to a duplicate name)
        // Only check if the name is DIFFERENT from current name
        if (!batch.getBatchName().equals(newName) &&
                batchRepo.existsByInstructorIdAndBatchName(batch.getInstructor().getId(), newName)) {
            throw new RuntimeException("You already have a class named '" + newName + "'");
        }

        // 3. Update Fields
        batch.setBatchName(newName);
        batch.setDescription(newDescription);
        // NEVER update the batchCode!

        Batch updated = batchRepo.save(batch);

        // 4. Return DTO
        long studentCount = assignmentRepo.countByBatchIdAndActiveTrue(batchId);
        long quizCount = quizRepo.countByBatchId(batchId);
        return new BatchResponseDTO(
                updated.getId(), updated.getBatchName(), updated.getBatchCode(), updated.getDescription(), studentCount,quizCount
        );
    }

    public BatchDetailsDto getBatchDetails(Long batchId, Long userId) {
        // 1. Find Batch & Verify Owner
        Batch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        if (!batch.getInstructor().getCustomer().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to this batch");
        }

        // 2. Fetch Students in this Batch
        List<InstructorStudentAssignment> assignments = assignmentRepo.findByBatchIdAndActiveTrue(batchId);
        List<StudentDto> studentDtos = assignments.stream()
                .map(a -> new StudentDto(
                        a.getStudent().getId(),
                        a.getStudent().getUsername(),
                        a.getStudent().getEmail() // Add whatever fields you need
                ))
                .collect(Collectors.toList());

        // 3. Fetch Quizzes in this Batch
        List<Quiz> quizzes = quizRepo.findByBatchId(batchId);
        List<QuizSummary> quizSummaries = quizzes.stream()
                .map(q -> new QuizSummary(q.getId(), q.getTitle(), q.getQuestions().size()))
                .collect(Collectors.toList());

        // 4. Build Response
        BatchDetailsDto response = new BatchDetailsDto();
        response.setId(batch.getId());
        response.setBatchName(batch.getBatchName());
        response.setBatchCode(batch.getBatchCode());
        response.setDescription(batch.getDescription());
        response.setStudents(studentDtos);
        response.setQuizzes(quizSummaries);

        return response;
    }
    // Helper: Generate simple code (e.g., "MATH-A1B2")
    private String generateBatchCode(String name) {
        String prefix = name.length() > 3 ? name.substring(0, 3).toUpperCase() : "CLS";
        String random = RandomStringUtils.randomAlphanumeric(4).toUpperCase();
        return prefix + "-" + random;
    }

    public BatchResponseDTO getBatchDetail(String batchCode) {
        return  batchRepo.findByBatchCode(batchCode).map(this::mapToDTO).orElseThrow(() -> new RuntimeException(
                "Batch not found with code: " + batchCode
        ));
    }
    private BatchResponseDTO mapToDTO(Batch batch) {
        BatchResponseDTO dto = new BatchResponseDTO();
        dto.setId(batch.getId());
        dto.setBatchCode(batch.getBatchCode());
        dto.setBatchName(batch.getBatchName());
        dto.setDescription(batch.getDescription());
        return dto;
    }

    // Inside BatchService

    // NEW: Get batches for a Student
    public List<BatchResponseDTO> getEnrolledBatches(Long studentId) {
        // 1. Find all active assignments for this student
        List<InstructorStudentAssignment> assignments = assignmentRepo.findByStudentIdAndActiveTrue(studentId);

        // 2. Map the Batch data to DTOs
        return assignments.stream()
                .map(assignment -> {
                    Batch b = assignment.getBatch();

                    // Optional: Count active quizzes for this batch to show on the card badge
                    long activeQuizCount = quizRepo.findActiveQuizzesForStudent(b.getId()).size();

                    return new BatchResponseDTO(
                            b.getId(),
                            b.getBatchName(),
                            b.getBatchCode(),
                            b.getDescription(),
                            0, // studentCount (not needed for student view)
                            activeQuizCount // Reuse quizCount field to show "Pending Quizzes"
                    );
                })
                .collect(Collectors.toList());
    }

    // Inside BatchService.java


    public List<StudentDto> getStudentsInBatch(Long batchId, String instructorUsername) {

        // 1. Fetch Batch & 2. Security Check (Keep your existing code here)
        Batch batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));
        Customer instructor = customerRepo.findByUsername(instructorUsername)
                .orElseThrow(() -> new RuntimeException("Instructor not found"));

        if (!batch.getInstructor().getCustomer().getId().equals(instructor.getId())) {
            throw new RuntimeException("Unauthorized: You do not own this batch");
        }

        // 3. Fetch Assignments
        List<InstructorStudentAssignment> assignments = assignmentRepo.findByBatchId(batchId);

        // 4. Map to DTO with Average Score Calculation
        return assignments.stream()
                .map(a -> {
                    StudentDto dto = new StudentDto();
                    dto.setId(a.getStudent().getId());
                    dto.setName(a.getStudent().getUsername());
                    dto.setEmail(a.getStudent().getEmail());
                    dto.setNotes(a.getNotes());
                    dto.setActive(a.isActive());

                    // --- CALCULATION START ---
                    // Fetch all quiz results for this student IN THIS BATCH
                    List<QResult> results = qResultRepo.findByStudentIdAndBatchId(a.getStudent().getId(), batchId);

                    if (results.isEmpty()) {
                        dto.setAverageScore(0.0);
                    } else {
                        // Calculate Average % (Score / TotalMarks * 100)
                        double totalPercentage = results.stream()
                                .mapToDouble(r -> {
                                    if (r.getTotalMarks() == 0) return 0.0;
                                    return ((double) r.getScoreObtained() / r.getTotalMarks()) * 100.0;
                                })
                                .sum();

                        double avg = totalPercentage / results.size();

                        // Round to 1 decimal place (e.g., 75.5)
                        dto.setAverageScore(Math.round(avg * 10.0) / 10.0);
                    }
                    // --- CALCULATION END ---

                    return dto;
                })
                .collect(Collectors.toList());
    }
    // BatchService.java

    @Transactional
    public void leaveBatch(Long studentId, Long batchId) {
        // 1. Find the Assignment
        InstructorStudentAssignment assignment = assignmentRepo.findByStudentIdAndBatchId(studentId, batchId)
                .orElseThrow(() -> new RuntimeException("You are not enrolled in this batch."));

        // 2. Soft Delete (Mark as Inactive)
        assignment.setActive(false);
        assignment.setNotes(assignment.getNotes() + " | Left on " + java.time.LocalDate.now());

        // 3. Save
        assignmentRepo.save(assignment);
    }
}
