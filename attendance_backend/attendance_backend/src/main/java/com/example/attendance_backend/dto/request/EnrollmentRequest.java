package com.example.attendance_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận yêu cầu đăng ký khóa học từ Sinh viên.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentRequest {

    @NotBlank(message = "Mã khóa học (courseId) không được để trống")
    private String courseId;

    /**
     * ID sinh viên (tùy chọn, nếu để trống hệ thống sẽ tự động gán từ sinh viên đang đăng nhập).
     */
    private String studentId;
}
