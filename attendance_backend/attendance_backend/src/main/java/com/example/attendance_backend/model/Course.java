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
 * Thực thể Course tương ứng với bảng courses.
 */
@Entity
@Table(name = "courses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @Column(name = "course_id", nullable = false)
    private String courseId;

    @Column(name = "subject_code")
    private String subjectCode;

    @Column(name = "class_name")
    private String className;

    @Column(name = "semester")
    private String semester;

    @Column(name = "lecturer_id")
    private String lecturerId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
