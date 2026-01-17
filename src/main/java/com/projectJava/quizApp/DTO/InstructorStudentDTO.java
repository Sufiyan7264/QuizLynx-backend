package com.projectJava.quizApp.DTO;

import com.projectJava.quizApp.model.InstructorStudentAssignment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InstructorStudentDTO {
    private Long studentId;
    private String username;
    private String email;
    private LocalDateTime joinedAt;
    private List<String> enrolledBatches;

    public static InstructorStudentDTO fromAssignment(InstructorStudentAssignment a) {
        InstructorStudentDTO d = new InstructorStudentDTO();
        d.setStudentId(a.getStudent().getId());
        d.setUsername(a.getStudent().getUsername());
        d.setEmail(a.getStudent().getEmail());
        d.setJoinedAt(a.getAssignedAt());
        return d;
    }
}
