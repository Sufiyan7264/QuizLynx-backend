package com.projectJava.quizApp.DTO;

import com.projectJava.quizApp.model.InstructorProfile;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data

public class InstructorDTO {
    private Long id;
    private Long userId;
    private String displayName;
    private  String bio;
    private List<String> subjects;
    private String instructorCode;
    private String avatarUrl;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    public static InstructorDTO from(InstructorProfile p) {
        InstructorDTO d = new InstructorDTO();
        d.id = p.getId();
        d.userId = p.getCustomer().getId();
        d.displayName = p.getDisplayName();
        d.bio = p.getBio();
        d.subjects = p.getSubjects() == null ? List.of() : List.of(p.getSubjects());
        d.instructorCode = p.getInstructorCode();
        d.avatarUrl = p.getAvatarUrl();
        d.createdAt = p.getCreatedAt();
        d.updatedAt = p.getUpdatedAt();
        return d;
    }

}
