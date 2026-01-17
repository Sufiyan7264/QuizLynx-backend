package com.projectJava.quizApp.controller;

import com.projectJava.quizApp.DTO.BindByCodeRequest;
import com.projectJava.quizApp.DTO.InstructorDTO;
import com.projectJava.quizApp.repo.UserRepo;
import com.projectJava.quizApp.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.Principal;
import java.util.List;
import java.util.Map;

public class StudentController {

    @Autowired
    private StudentService studentService;
    @Autowired
    private UserRepo userRepo;
    // API 1: Get My Instructors (Returns a List)
    @GetMapping("/instructors")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<InstructorDTO>> getMyInstructors(Principal principal) {
        Long studentId = getUserId(principal);

        List<InstructorDTO> instructors = studentService.getMyInstructor(studentId);

        if (instructors.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(instructors);
    }

    // API 2: Join an Instructor
// API 2: Join a Class
    @PostMapping("/join")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> joinClass(@RequestBody BindByCodeRequest req, Principal principal) {
        Long studentId = getUserId(principal);

        // Calls the NEW joinBatch method
        studentService.joinBatch(studentId, req.getCode());

        return ResponseEntity.ok(Map.of("message", "Successfully joined the class!"));
    }

    private Long getUserId(Principal principal) {
        return userRepo.findByUsername(principal.getName()).orElseThrow().getId();
    }
}
