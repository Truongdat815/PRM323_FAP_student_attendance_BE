package com.example.attendance_backend.service;

import com.example.attendance_backend.dto.request.EnrollmentRequest;
import com.example.attendance_backend.dto.response.EnrollmentResponse;

import java.io.IOException;
import java.util.List;

/**
 * Interface nghiệp vụ quản lý đăng ký khóa học (Enrollments).
 */
public interface EnrollmentService {

    EnrollmentResponse enrollCourse(EnrollmentRequest request, String currentUsername) throws IOException;

    List<EnrollmentResponse> getMyEnrollments(String currentUsername) throws IOException;

    List<EnrollmentResponse> getStudentsByCourse(String courseId, String currentUsername) throws IOException;
}
