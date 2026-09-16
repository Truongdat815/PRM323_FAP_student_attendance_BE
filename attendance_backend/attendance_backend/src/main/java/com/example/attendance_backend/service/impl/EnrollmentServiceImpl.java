package com.example.attendance_backend.service.impl;

import com.example.attendance_backend.dto.request.EnrollmentRequest;
import com.example.attendance_backend.dto.response.EnrollmentResponse;
import com.example.attendance_backend.exception.AppException;
import com.example.attendance_backend.model.Course;
import com.example.attendance_backend.model.Enrollment;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.CourseRepository;
import com.example.attendance_backend.repository.EnrollmentRepository;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.service.EnrollmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lớp triển khai nghiệp vụ đăng ký khóa học cho Sinh viên.
 */
@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentServiceImpl.class);

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Autowired
    public EnrollmentServiceImpl(EnrollmentRepository enrollmentRepository,
                                 CourseRepository courseRepository,
                                 UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @Override
    public EnrollmentResponse enrollCourse(EnrollmentRequest request, String currentUsername) throws IOException {
        User student = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Tài khoản hiện tại không tồn tại trong hệ thống."));

        // 1. Kiểm tra vai trò: Chỉ sinh viên (hoặc admin) mới được đăng ký
        if (!"ROLE_STUDENT".equalsIgnoreCase(student.getRoleId()) && !"ROLE_ADMIN".equalsIgnoreCase(student.getRoleId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Chỉ tài khoản sinh viên mới có quyền đăng ký khóa học.");
        }

        String courseId = request.getCourseId().trim();

        // 2. Kiểm tra khóa học có tồn tại trong sheet COURSES không
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với mã: " + courseId));

        // 3. Kiểm tra đăng ký trùng lặp: Sinh viên chỉ được đăng ký 1 lần cho mỗi khóa học
        if (enrollmentRepository.existsByCourseIdAndStudentId(courseId, student.getUserId())) {
            throw new AppException(HttpStatus.CONFLICT, "Bạn đã đăng ký tham gia lớp học phần này trước đó.");
        }

        // 4. Tạo bản ghi đăng ký mới
        String enrollmentId = "ENR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String now = Instant.now().toString();

        Enrollment newEnrollment = Enrollment.builder()
                .enrollmentId(enrollmentId)
                .courseId(courseId)
                .studentId(student.getUserId())
                .enrolledAt(now)
                .build();

        enrollmentRepository.save(newEnrollment);
        log.info("✅ Sinh viên '{}' (fap_code={}) đã đăng ký thành công khóa học: courseId={}, class={}",
                student.getUsername(), student.getFapCode(), courseId, course.getClassName());

        return EnrollmentResponse.builder()
                .enrollmentId(enrollmentId)
                .courseId(courseId)
                .studentId(student.getUserId())
                .enrolledAt(now)
                .subjectCode(course.getSubjectCode())
                .className(course.getClassName())
                .semester(course.getSemester())
                .studentName(student.getFullName())
                .fapCode(student.getFapCode())
                .build();
    }

    @Override
    public List<EnrollmentResponse> getMyEnrollments(String currentUsername) throws IOException {
        User student = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại."));

        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getUserId());
        List<EnrollmentResponse> responses = new ArrayList<>();

        for (Enrollment e : enrollments) {
            EnrollmentResponse res = EnrollmentResponse.fromEnrollment(e);
            Optional<Course> courseOpt = courseRepository.findById(e.getCourseId());
            courseOpt.ifPresent(course -> {
                res.setSubjectCode(course.getSubjectCode());
                res.setClassName(course.getClassName());
                res.setSemester(course.getSemester());
            });
            res.setStudentName(student.getFullName());
            res.setFapCode(student.getFapCode());
            responses.add(res);
        }

        return responses;
    }

    @Override
    public List<EnrollmentResponse> getStudentsByCourse(String courseId, String currentUsername) throws IOException {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với mã: " + courseId));

        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
        List<EnrollmentResponse> responses = new ArrayList<>();

        for (Enrollment e : enrollments) {
            EnrollmentResponse res = EnrollmentResponse.fromEnrollment(e);
            res.setSubjectCode(course.getSubjectCode());
            res.setClassName(course.getClassName());
            res.setSemester(course.getSemester());

            Optional<User> studentOpt = userRepository.findById(e.getStudentId());
            studentOpt.ifPresent(student -> {
                res.setStudentName(student.getFullName());
                res.setFapCode(student.getFapCode());
            });
            responses.add(res);
        }

        return responses;
    }
}
