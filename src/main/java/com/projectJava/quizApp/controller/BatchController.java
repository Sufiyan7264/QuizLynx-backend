package com.projectJava.quizApp.controller;

import com.projectJava.quizApp.DTO.BatchDetailsDto;
import com.projectJava.quizApp.DTO.BatchResponseDTO;
import com.projectJava.quizApp.DTO.StudentBatchDto;
import com.projectJava.quizApp.DTO.StudentDto;
import com.projectJava.quizApp.model.Batch;
import com.projectJava.quizApp.repo.UserRepo;
import com.projectJava.quizApp.service.BatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    @Autowired
    private BatchService batchService;

    @Autowired
    private UserRepo userRepo;


    // 1. Create a Class (e.g., "Class 8")
    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> createBatch(@RequestBody Map<String, String> req, Principal principal) {
        Long userId = getUserId(principal);
        String name = req.get("batchName"); // Expect JSON: { "batchName": "Class 8" }
        String description = req.get("description"); // Expect JSON: { "batchName": "Class 8" }

        BatchResponseDTO batch = batchService.createBatch(userId, name,description);

        return ResponseEntity.ok(Map.of(
                "message", "Class created successfully",
                "batchName", batch.getBatchName(),
                "batchCode", batch.getBatchCode()
        ));
    }

    // 2. List all my Classes
    @GetMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<List<BatchResponseDTO>> getMyBatches(Principal principal) {
        Long userId = getUserId(principal);
        return ResponseEntity.ok(batchService.getMyBatches(userId));
    }
    @DeleteMapping("/{batchId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> deleteBatch(@PathVariable Long batchId,
                                         @RequestParam(defaultValue = "false") boolean force,
                                         Principal principal) {
        Long userId = getUserId(principal);

        try {
            batchService.deleteBatch(batchId, userId, force);
            return ResponseEntity.ok(Map.of("message", "Class deleted successfully"));

        } catch (RuntimeException e) {
            // If it's our specific warning, send a specific status (e.g., 409 Conflict)
            if (e.getMessage().startsWith("DependencyWarning")) {
                return ResponseEntity.status(HttpStatus.CONFLICT) // 409 means "Conflict with current state"
                        .body(Map.of("error", "DEPENDENCY_ERROR", "message", e.getMessage()));
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Inside BatchController.java

    @PutMapping("/{batchId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> updateBatch(@PathVariable Long batchId,
                                         @RequestBody Map<String,String> req, // Create a simple DTO or use Map
                                         Principal principal) {
        Long userId = getUserId(principal);
        String batchName = req.get("batchName");
        String description = req.get("description");

        return ResponseEntity.ok(
                batchService.updateBatch(batchId, userId, batchName, description)
        );
    }
    // Inside BatchController.java

    @GetMapping("/student/{batchId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentBatchDto> getBatchViewForStudent(@PathVariable Long batchId, Principal principal) {
        Long studentId = getUserId(principal); // Reuse your helper method
        return ResponseEntity.ok(batchService.getBatchDetailsForStudent(batchId, studentId));
    }

    @GetMapping("/{batchId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<BatchDetailsDto> getBatchDetails(@PathVariable Long batchId, Principal principal) {
        Long userId = getUserId(principal);

        return ResponseEntity.ok(batchService.getBatchDetails(batchId, userId));
    }
    private Long getUserId(Principal principal) {
        return userRepo.findByUsername(principal.getName()).orElseThrow().getId();
    }

    // Inside BatchController

    @GetMapping("/student/my-batches")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<BatchResponseDTO>> getEnrolledBatches(Principal principal) {
        Long studentId = getUserId(principal);
        return ResponseEntity.ok(batchService.getEnrolledBatches(studentId));
    }

    @GetMapping("/code/{batchCode}")
    public ResponseEntity<BatchResponseDTO> getBatchDetail(@PathVariable String batchCode) {
        return ResponseEntity.ok(batchService.getBatchDetail(batchCode));
    }

    @GetMapping("/{batchId}/students")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<List<StudentDto>> getBatchStudents(
            @PathVariable Long batchId,
            Principal principal) {

        return ResponseEntity.ok(batchService.getStudentsInBatch(batchId, principal.getName()));
    }

    // 3. Student Leaves a Batch
    @PostMapping("/student/leave/{batchId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> leaveBatch(@PathVariable Long batchId, Principal principal) {
        Long studentId = getUserId(principal);

        batchService.leaveBatch(studentId, batchId);

        return ResponseEntity.ok(Map.of("message", "You have successfully left the class."));
    }
}
