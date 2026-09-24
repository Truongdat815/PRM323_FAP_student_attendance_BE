package com.example.attendance_backend.controller;

import com.example.attendance_backend.dto.request.CourseRegisterRequest;
import com.example.attendance_backend.dto.request.CourseRequest;
import com.example.attendance_backend.dto.response.ApiResponse;
import com.example.attendance_backend.dto.response.CourseResponse;
import com.example.attendance_backend.service.CourseService;
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
 * Controller quản lý học phần và lớp học phần (CRUD Courses).
 */
@RestController
@RequestMapping("/api/courses")
@Tag(name = "Course Controller", description = "Các API quản lý khóa học và lớp học phần (CRUD Courses)")
@SecurityRequirement(name = "Bearer Authentication")
public class CourseController {

    private final CourseService courseService;

    @Autowired
    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách khóa học", description = "Lấy tất cả khóa học hoặc lọc theo giảng viên (lecturerId)")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getAllCourses(
            @RequestParam(required = false) String lecturerId) throws IOException {
        List<CourseResponse> courses = courseService.getAllCourses(lecturerId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách khóa học thành công", courses));
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm khóa học", description = "Tìm kiếm khóa học theo className, subjectCode, semester và tên của giảng viên (các tiêu chí đều là tùy chọn và có thể kết hợp)")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> searchCourses(
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String lecturerName) throws IOException {
        List<CourseResponse> courses = courseService.searchCourses(className, subjectCode, semester, lecturerName);
        return ResponseEntity.ok(ApiResponse.ok("Tìm kiếm khóa học thành công", courses));
    }

    @GetMapping("/my-courses")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Lấy danh sách khóa học của học sinh", description = "Trả về danh sách các khóa học mà học sinh đã đăng ký tham gia, kèm theo tên giảng viên. Nếu không truyền studentId thì lấy theo người dùng đang đăng nhập.")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getEnrolledCourses(
            @RequestParam(required = false) String studentId,
            Authentication authentication) throws IOException {
        String targetStudentId = (studentId != null && !studentId.trim().isEmpty())
                ? studentId.trim()
                : authentication.getName();
        List<CourseResponse> courses = courseService.getEnrolledCourses(targetStudentId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách khóa học của học sinh thành công", courses));
    }

    @PostMapping({"/register", "/enroll"})
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Học sinh đăng ký khóa học", description = "Học sinh đăng ký tham gia khóa học bằng mã khóa học (courseId), mã học sinh (userId) và mật khẩu khóa học (coursePassword)")
    public ResponseEntity<ApiResponse<CourseResponse>> registerCourse(
            @Valid @RequestBody CourseRegisterRequest request,
            Authentication authentication) throws IOException {
        CourseResponse response = courseService.registerCourse(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Đăng ký khóa học thành công", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết khóa học", description = "Lấy thông tin chi tiết một khóa học theo courseId")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseById(@PathVariable String id) throws IOException {
        CourseResponse course = courseService.getCourseById(id);
        return ResponseEntity.ok(ApiResponse.ok("Lấy thông tin khóa học thành công", course));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    @Operation(summary = "Tạo khóa học mới", description = "Dành cho Giảng viên tạo lớp học phần mới trên Google Sheets")
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CourseRequest request,
            Authentication authentication) throws IOException {
        CourseResponse response = courseService.createCourse(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo khóa học thành công", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    @Operation(summary = "Cập nhật khóa học", description = "Giảng viên cập nhật thông tin lớp học do mình phụ trách")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @PathVariable String id,
            @Valid @RequestBody CourseRequest request,
            Authentication authentication) throws IOException {
        CourseResponse response = courseService.updateCourse(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin khóa học thành công", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    @Operation(summary = "Xóa khóa học", description = "Giảng viên xóa lớp học do mình phụ trách khỏi Google Sheets")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(
            @PathVariable String id,
            Authentication authentication) throws IOException {
        courseService.deleteCourse(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Xóa khóa học thành công", null));
    }
}
