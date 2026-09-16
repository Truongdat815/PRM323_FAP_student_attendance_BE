package com.example.attendance_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận dữ liệu tạo mới hoặc cập nhật khóa học từ Giảng viên.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequest {

    @NotBlank(message = "Mã môn học không được để trống (ví dụ: PRM392)")
    private String subjectCode;

    @NotBlank(message = "Tên lớp học phần không được để trống (ví dụ: SE1701)")
    private String className;

    @NotBlank(message = "Học kỳ không được để trống (ví dụ: FA24, SP25)")
    private String semester;

    /**
     * ID của giảng viên (nếu bỏ trống, hệ thống sẽ tự động lấy từ giảng viên đang đăng nhập).
     */
    private String lecturerId;
}
