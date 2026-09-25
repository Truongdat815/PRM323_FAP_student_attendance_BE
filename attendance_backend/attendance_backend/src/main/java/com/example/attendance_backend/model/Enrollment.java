package com.example.attendance_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Thực thể Enrollment tương ứng với bảng enrollments.
 */
@Entity
@Table(name = "enrollments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @Id
    @Column(name = "enrollment_id", nullable = false)
    private String enrollmentId;

    @Column(name = "course_id")
    private String courseId;

    @Column(name = "student_id")
    private String studentId;

    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;
}
