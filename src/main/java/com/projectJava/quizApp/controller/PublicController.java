package com.projectJava.quizApp.controller;

import com.projectJava.quizApp.DTO.ContactRequestDto;
import com.projectJava.quizApp.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired private ContactService contactService;

    @PostMapping("/contact")
    public ResponseEntity<?> contactUser(@RequestBody ContactRequestDto dto){
        if (dto.getEmail() == null || dto.getMessage() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and Message are required"));
        }
        try {
            contactService.sendContactEmail(dto);
            return ResponseEntity.ok(Map.of("message", "Message sent successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to send email. Please try again later."));
        }
    }
}
