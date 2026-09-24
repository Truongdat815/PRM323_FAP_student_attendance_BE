package com.example.attendance_backend.service.impl;

import com.example.attendance_backend.dto.request.CourseRegisterRequest;
import com.example.attendance_backend.dto.request.CourseRequest;
import com.example.attendance_backend.dto.response.CourseResponse;
import com.example.attendance_backend.exception.AppException;
import com.example.attendance_backend.model.Course;
import com.example.attendance_backend.model.Enrollment;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.CourseRepository;
import com.example.attendance_backend.repository.EnrollmentRepository;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.service.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ CRUD khóa học cho Giảng viên.
 */
@Service
public class CourseServiceImpl implements CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public CourseServiceImpl(CourseRepository courseRepository,
                             UserRepository userRepository,
                             EnrollmentRepository enrollmentRepository,
                             PasswordEncoder passwordEncoder) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public CourseServiceImpl(CourseRepository courseRepository, UserRepository userRepository) {
        this(courseRepository, userRepository, null, null);
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

        Course newCourse = Course.builder()
                .courseId(courseId)
                .subjectCode(request.getSubjectCode().trim().toUpperCase())
                .className(request.getClassName().trim().toUpperCase())
                .semester(request.getSemester().trim().toUpperCase())
                .lecturerId(lecturerId)
                .password(passwordEncoder.encode(request.getPassword().trim()))
                .createdAt(LocalDateTime.now())
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

        Course updatedCourse = courseRepository.save(existingCourse);

        log.info("✅ Cập nhật thông tin khóa học thành công: courseId={}", courseId);
        return CourseResponse.fromCourse(updatedCourse);
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

        courseRepository.delete(existingCourse);

        log.info("✅ Giảng viên '{}' đã xóa khóa học: courseId={}", currentUsername, courseId);
    }

    @Override
    public CourseResponse registerCourse(CourseRegisterRequest request, String currentUsername) throws IOException {
        String targetUserId = (request.getUserId() != null && !request.getUserId().trim().isEmpty())
                ? request.getUserId().trim()
                : currentUsername;

        User student = userRepository.findById(targetUserId)
                .or(() -> userRepository.findByUsername(targetUserId))
                .or(() -> userRepository.findByFapCode(targetUserId))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học sinh với mã: " + targetUserId));

        // Kiểm tra vai trò: Chỉ sinh viên (hoặc admin) mới được đăng ký
        String role = student.getRoleId();
        boolean isStudent = role != null && (role.equalsIgnoreCase("STUDENT") || role.equalsIgnoreCase("ROLE_STUDENT"));
        boolean isAdmin = role != null && (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("ROLE_ADMIN"));
        if (!isStudent && !isAdmin) {
            throw new AppException(HttpStatus.FORBIDDEN, "Chỉ tài khoản sinh viên mới có quyền đăng ký khóa học.");
        }

        String courseId = request.getCourseId() != null ? request.getCourseId().trim() : "";
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với mã: " + courseId));

        // Kiểm tra mật khẩu khóa học (nếu khóa học có thiết lập mật khẩu)
        if (course.getPassword() != null && !course.getPassword().trim().isEmpty()) {
            String expectedPassword = course.getPassword().trim();
            String providedPassword = (request.getCoursePassword() != null) ? request.getCoursePassword().trim() : "";

            boolean matches = expectedPassword.equals(providedPassword);
            if (!matches && passwordEncoder != null) {
                matches = passwordEncoder.matches(providedPassword, expectedPassword);
            }

            if (!matches) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Mật khẩu khóa học không chính xác.");
            }
        }

        // Kiểm tra đăng ký trùng lặp
        if (enrollmentRepository.existsByCourseIdAndStudentId(course.getCourseId(), student.getUserId())) {
            throw new AppException(HttpStatus.CONFLICT, "Học sinh đã đăng ký tham gia khóa học này trước đó.");
        }

        // Tạo bản ghi Enrollment
        String enrollmentId = "ENR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Enrollment enrollment = Enrollment.builder()
                .enrollmentId(enrollmentId)
                .courseId(course.getCourseId())
                .studentId(student.getUserId())
                .enrolledAt(LocalDateTime.now())
                .build();
        enrollmentRepository.save(enrollment);

        log.info("✅ Học sinh '{}' (userId={}) đăng ký thành công khóa học: courseId={}, className={}",
                student.getUsername(), student.getUserId(), course.getCourseId(), course.getClassName());

        // Lấy tên giảng viên nếu có
        String lecturerName = null;
        if (course.getLecturerId() != null) {
            lecturerName = userRepository.findById(course.getLecturerId())
                    .map(User::getFullName)
                    .orElse(null);
        }

        return CourseResponse.fromCourse(course, lecturerName);
    }

    @Override
    public List<CourseResponse> searchCourses(String className, String subjectCode, String semester, String lecturerName) throws IOException {
        String cleanClassName = (className != null && !className.trim().isEmpty()) ? className.trim() : null;
        String cleanSubjectCode = (subjectCode != null && !subjectCode.trim().isEmpty()) ? subjectCode.trim() : null;
        String cleanSemester = (semester != null && !semester.trim().isEmpty()) ? semester.trim() : null;
        String cleanLecturerName = (lecturerName != null && !lecturerName.trim().isEmpty()) ? lecturerName.trim() : null;

        List<Course> courses = courseRepository.searchCourses(cleanClassName, cleanSubjectCode, cleanSemester, cleanLecturerName);

        Set<String> lecturerIds = courses.stream()
                .map(Course::getLecturerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, String> lecturerMap = new HashMap<>();
        if (!lecturerIds.isEmpty()) {
            lecturerMap = userRepository.findAllById(lecturerIds).stream()
                    .collect(Collectors.toMap(User::getUserId, User::getFullName, (oldVal, newVal) -> oldVal));
        }

        Map<String, String> finalLecturerMap = lecturerMap;
        return courses.stream()
                .map(c -> CourseResponse.fromCourse(c, finalLecturerMap.get(c.getLecturerId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponse> getEnrolledCourses(String studentId) throws IOException {
        // Tìm học sinh
        User student = userRepository.findById(studentId)
                .or(() -> userRepository.findByUsername(studentId))
                .or(() -> userRepository.findByFapCode(studentId))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học sinh với mã: " + studentId));

        // Lấy danh sách enrollment của học sinh
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getUserId());
        if (enrollments.isEmpty()) {
            return Collections.emptyList();
        }

        // Lấy danh sách courseId từ enrollments
        List<String> courseIds = enrollments.stream()
                .map(Enrollment::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Batch-fetch tất cả courses
        List<Course> courses = courseRepository.findAllById(courseIds);

        // Batch-fetch tất cả tên giảng viên
        Set<String> lecturerIds = courses.stream()
                .map(Course::getLecturerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, String> lecturerMap = new HashMap<>();
        if (!lecturerIds.isEmpty()) {
            lecturerMap = userRepository.findAllById(lecturerIds).stream()
                    .collect(Collectors.toMap(User::getUserId, User::getFullName, (oldVal, newVal) -> oldVal));
        }

        Map<String, String> finalLecturerMap = lecturerMap;
        return courses.stream()
                .map(c -> CourseResponse.fromCourse(c, finalLecturerMap.get(c.getLecturerId())))
                .collect(Collectors.toList());
    }

    private User getCurrentUser(String username) throws IOException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Người dùng hiện tại không tồn tại trong hệ thống."));
    }
}
