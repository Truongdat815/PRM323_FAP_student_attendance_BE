package com.example.attendance_backend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận dữ liệu học sinh đăng ký khóa học (Course Registration).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRegisterRequest {

    @NotBlank(message = "Mã khóa học (courseId) không được để trống")
    private String courseId;

    @NotBlank(message = "Mã học sinh (userId) không được để trống")
    @JsonAlias({"studentId", "user_id", "student_id"})
    private String userId;

    @JsonAlias({"password", "course_password"})
    private String coursePassword;
}
