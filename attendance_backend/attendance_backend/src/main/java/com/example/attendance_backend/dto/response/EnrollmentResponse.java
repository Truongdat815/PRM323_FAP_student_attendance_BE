package com.example.attendance_backend.dto.response;

import com.example.attendance_backend.model.Enrollment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO trả về thông tin đăng ký khóa học của sinh viên.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {

    private String enrollmentId;
    private String courseId;
    private String studentId;
    private String enrolledAt;

    // Các thông tin mở rộng hỗ trợ hiển thị
    private String subjectCode;
    private String className;
    private String semester;
    private String studentName;
    private String fapCode;

    public static EnrollmentResponse fromEnrollment(Enrollment enrollment) {
        if (enrollment == null) {
            return null;
        }
        return EnrollmentResponse.builder()
                .enrollmentId(enrollment.getEnrollmentId())
                .courseId(enrollment.getCourseId())
                .studentId(enrollment.getStudentId())
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }
}
