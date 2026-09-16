package com.example.attendance_backend.controller;

import com.example.attendance_backend.dto.request.EnrollmentRequest;
import com.example.attendance_backend.dto.response.ApiResponse;
import com.example.attendance_backend.dto.response.EnrollmentResponse;
import com.example.attendance_backend.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * Controller tiếp nhận yêu cầu đăng ký khóa học và tra cứu danh sách lớp học.
 */
@RestController
@RequestMapping("/api/enrollments")
@Tag(name = "Enrollment Controller", description = "Các API đăng ký và quản lý danh sách sinh viên tham gia khóa học")
@SecurityRequirement(name = "Bearer Authentication")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @Autowired
    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Đăng ký tham gia khóa học", description = "Dành cho Sinh viên đăng ký tham gia vào một lớp học phần (tránh đăng ký trùng lặp)")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enrollCourse(
            @Valid @RequestBody EnrollmentRequest request,
            Authentication authentication) throws IOException {
        EnrollmentResponse response = enrollmentService.enrollCourse(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đăng ký khóa học thành công", response));
    }

    @GetMapping("/my-courses")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Xem các khóa học đã đăng ký", description = "Sinh viên xem danh sách các lớp học phần mà mình đang theo học")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getMyEnrollments(
            Authentication authentication) throws IOException {
        List<EnrollmentResponse> responses = enrollmentService.getMyEnrollments(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách khóa học đã đăng ký thành công", responses));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    @Operation(summary = "Danh sách sinh viên trong lớp học", description = "Giảng viên xem danh sách sinh viên đã đăng ký tham gia lớp học phần")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getStudentsByCourse(
            @PathVariable String courseId,
            Authentication authentication) throws IOException {
        List<EnrollmentResponse> responses = enrollmentService.getStudentsByCourse(courseId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách sinh viên trong lớp học thành công", responses));
    }
}
