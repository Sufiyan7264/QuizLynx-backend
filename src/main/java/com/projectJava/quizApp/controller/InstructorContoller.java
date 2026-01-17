package com.projectJava.quizApp.controller;

import com.projectJava.quizApp.DTO.BindByCodeRequest;
import com.projectJava.quizApp.DTO.CreateInstructorProfileRequest;
import com.projectJava.quizApp.DTO.InstructorDTO;
import com.projectJava.quizApp.DTO.InstructorStudentDTO;
import com.projectJava.quizApp.model.Customer;
import com.projectJava.quizApp.model.InstructorProfile;
import com.projectJava.quizApp.model.InstructorStudentAssignment;
import com.projectJava.quizApp.repo.UserRepo;
import com.projectJava.quizApp.service.InstructorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/instructor")
public class InstructorContoller {

    @Autowired
    private InstructorService service;
    @Autowired
    private UserRepo userRepo;

    @GetMapping("/me")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> getMyProfile(Principal principal){
        Long userId = getUserIdFromPrincipal(principal);
        return  service.findByUserId(userId)
                .map(p->ResponseEntity.ok(InstructorDTO.from(p)))
                .orElseGet(()->ResponseEntity.noContent().build());
    }

    @PostMapping("/me")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<InstructorDTO> createProfile(Principal principal, @Valid @RequestBody CreateInstructorProfileRequest request){
        Long userId = getUserIdFromPrincipal(principal);
        InstructorProfile profile = service.createProfile(userId, request);
        return  ResponseEntity.status(HttpStatus.CREATED).body(InstructorDTO.from(profile));
    }

    // 2. Remove a Student
    @DeleteMapping("/students/{studentId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> removeStudent(@PathVariable Long studentId, Principal principal) {
        Long instructorUserId = getUserIdFromPrincipal(principal);

        service.removeStudent(instructorUserId, studentId);

        return ResponseEntity.ok(Map.of("message", "Student removed successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<InstructorDTO> getById(@PathVariable("id") Long id){
        return service.findById(id)
                .map(InstructorDTO::from).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PutMapping("/me")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<?> updateProfile(@RequestBody CreateInstructorProfileRequest req, Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        InstructorProfile updatedProfile = service.updateProfile(userId, req);
        return ResponseEntity.ok(InstructorDTO.from(updatedProfile));
    }
    @PostMapping("/bind-by-code")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> bindByCode(Principal principal, @RequestBody BindByCodeRequest req) {
        Long studentId = getUserIdFromPrincipal(principal);

        // Updated: Calls joinBatch instead of the old bindStudent logic
        // The service handles finding the batch and the instructor automatically
        service.joinBatch(studentId, req.getCode());

        return ResponseEntity.ok(Map.of("message", "Successfully joined the class!"));
    }
    // Inside your Controller

    @GetMapping("/students")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<List<InstructorStudentDTO>> getMyStudents(Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);

        return service.findByUserId(userId)
                .map(profile -> {
                    // 1. Fetch all raw assignments (e.g., Student A in Batch 1, Student A in Batch 2)
                    List<InstructorStudentAssignment> allAssignments = service.getStudents(profile.getId());

                    // 2. Group by Student ID so we don't have duplicates
                    Map<Long, List<InstructorStudentAssignment>> groupedByStudent = allAssignments.stream()
                            .collect(Collectors.groupingBy(a -> a.getStudent().getId()));

                    // 3. Convert groups into DTOs
                    List<InstructorStudentDTO> studentDtos = groupedByStudent.values().stream()
                            .map(assignments -> {
                                // Get student details from the first assignment in the list
                                InstructorStudentAssignment first = assignments.get(0);
                                Customer student = first.getStudent();

                                InstructorStudentDTO dto = new InstructorStudentDTO();
                                dto.setStudentId(student.getId());
                                dto.setUsername(student.getUsername()); // or student.getFullName()
                                dto.setEmail(student.getEmail());
//                                dto.setActive(first.isActive()); // Assuming active status is consistent

                                // 4. Extract all Batch Names for this student
                                List<String> batches = assignments.stream()
                                        .map(a -> a.getBatch().getBatchName())
                                        .collect(Collectors.toList());

                                dto.setEnrolledBatches(batches);

                                return dto;
                            })
                            .collect(Collectors.toList());

                    return ResponseEntity.ok(studentDtos);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    private Long getUserIdFromPrincipal(Principal principal){
        return userRepo.findByUsername(principal.getName()).orElseThrow().getId();
    }
}
