package com.example.attendance_backend.service.impl;

import com.example.attendance_backend.dto.request.CourseRequest;
import com.example.attendance_backend.dto.response.CourseResponse;
import com.example.attendance_backend.exception.AppException;
import com.example.attendance_backend.model.Course;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.CourseRepository;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.service.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ CRUD khóa học cho Giảng viên.
 */
@Service
public class CourseServiceImpl implements CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Autowired
    public CourseServiceImpl(CourseRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<CourseResponse> getAllCourses(String lecturerId) throws IOException {
        List<Course> courses;
        if (lecturerId != null && !lecturerId.trim().isEmpty()) {
            courses = courseRepository.findByLecturerId(lecturerId.trim());
        } else {
            courses = courseRepository.findAll();
        }

        return courses.stream()
                .map(CourseResponse::fromCourse)
                .collect(Collectors.toList());
    }

    @Override
    public CourseResponse getCourseById(String courseId) throws IOException {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với mã: " + courseId));
        return CourseResponse.fromCourse(course);
    }

    @Override
    public CourseResponse createCourse(CourseRequest request, String currentUsername) throws IOException {
        User currentUser = getCurrentUser(currentUsername);

        // Kiểm tra quyền hạn: phải là LECTURER hoặc ADMIN
        if (!"ROLE_LECTURER".equalsIgnoreCase(currentUser.getRoleId()) && !"ROLE_ADMIN".equalsIgnoreCase(currentUser.getRoleId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Chỉ giảng viên mới có quyền tạo khóa học mới.");
        }

        String lecturerId = (request.getLecturerId() != null && !request.getLecturerId().trim().isEmpty())
                ? request.getLecturerId().trim()
                : currentUser.getUserId();

        String courseId = "CRS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String now = Instant.now().toString();

        Course newCourse = Course.builder()
                .courseId(courseId)
                .subjectCode(request.getSubjectCode().trim().toUpperCase())
                .className(request.getClassName().trim().toUpperCase())
                .semester(request.getSemester().trim().toUpperCase())
                .lecturerId(lecturerId)
                .createdAt(now)
                .build();

        courseRepository.save(newCourse);
        log.info("✅ Giảng viên '{}' đã tạo khóa học thành công: courseId={}, subjectCode={}, class={}",
                currentUsername, courseId, newCourse.getSubjectCode(), newCourse.getClassName());

        return CourseResponse.fromCourse(newCourse);
    }

    @Override
    public CourseResponse updateCourse(String courseId, CourseRequest request, String currentUsername) throws IOException {
        Course existingCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với mã: " + courseId));

        User currentUser = getCurrentUser(currentUsername);

        // Kiểm tra quyền: chỉ giảng viên tạo khóa học hoặc admin mới được sửa
        if (!currentUser.getUserId().equalsIgnoreCase(existingCourse.getLecturerId()) &&
                !"ROLE_ADMIN".equalsIgnoreCase(currentUser.getRoleId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền chỉnh sửa khóa học do giảng viên khác phụ trách.");
        }

        existingCourse.setSubjectCode(request.getSubjectCode().trim().toUpperCase());
        existingCourse.setClassName(request.getClassName().trim().toUpperCase());
        existingCourse.setSemester(request.getSemester().trim().toUpperCase());

        boolean updated = courseRepository.update(courseId, existingCourse);
        if (!updated) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể cập nhật thông tin khóa học trên Google Sheets.");
        }

        log.info("✅ Cập nhật thông tin khóa học thành công: courseId={}", courseId);
        return CourseResponse.fromCourse(existingCourse);
    }

    @Override
    public void deleteCourse(String courseId, String currentUsername) throws IOException {
        Course existingCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với mã: " + courseId));

        User currentUser = getCurrentUser(currentUsername);

        // Kiểm tra quyền: chỉ giảng viên tạo khóa học hoặc admin mới được xóa
        if (!currentUser.getUserId().equalsIgnoreCase(existingCourse.getLecturerId()) &&
                !"ROLE_ADMIN".equalsIgnoreCase(currentUser.getRoleId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa khóa học do giảng viên khác phụ trách.");
        }

        boolean deleted = courseRepository.delete(courseId);
        if (!deleted) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xóa khóa học trên Google Sheets.");
        }

        log.info("✅ Giảng viên '{}' đã xóa khóa học: courseId={}", currentUsername, courseId);
    }

    private User getCurrentUser(String username) throws IOException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Người dùng hiện tại không tồn tại trong hệ thống."));
    }
}
