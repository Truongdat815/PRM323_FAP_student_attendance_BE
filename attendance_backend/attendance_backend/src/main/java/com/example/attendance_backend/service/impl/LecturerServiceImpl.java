package com.example.attendance_backend.service.impl;

import com.example.attendance_backend.dto.response.LecturerDashboardStatsResponse;
import com.example.attendance_backend.dto.response.RecentCourseResponse;
import com.example.attendance_backend.exception.AppException;
import com.example.attendance_backend.model.Course;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.AttendanceSessionRepository;
import com.example.attendance_backend.repository.CourseRepository;
import com.example.attendance_backend.repository.EnrollmentRepository;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.service.LecturerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ cho các tính năng thống kê và quản lý của Giảng viên.
 */
@Service
public class LecturerServiceImpl implements LecturerService {

    private static final Logger log = LoggerFactory.getLogger(LecturerServiceImpl.class);

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;

    @Autowired
    public LecturerServiceImpl(UserRepository userRepository,
                               CourseRepository courseRepository,
                               EnrollmentRepository enrollmentRepository,
                               AttendanceSessionRepository attendanceSessionRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
    }

    @Override
    public LecturerDashboardStatsResponse getDashboardStats(String currentUsername) throws IOException {
        return getDashboardStats(currentUsername, null);
    }

    @Override
    public LecturerDashboardStatsResponse getDashboardStats(String currentUsername, String lecturerId) throws IOException {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Người dùng hiện tại không tồn tại trong hệ thống."));

        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(currentUser.getRoleId())
                || "ADMIN".equalsIgnoreCase(currentUser.getRoleId());

        String targetLecturerId;
        if (lecturerId != null && !lecturerId.trim().isEmpty() && isAdmin) {
            targetLecturerId = lecturerId.trim();
        } else {
            targetLecturerId = currentUser.getUserId();
        }

        // Lấy tất cả các khóa học do giảng viên này phụ trách
        List<Course> courses = courseRepository.findByLecturerId(targetLecturerId);
        long totalCourses = courses.size();

        List<String> courseIds = courses.stream()
                .map(Course::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Tổng số lượt sinh viên đăng ký vào các khóa học
        long totalRegisteredStudents = courseIds.isEmpty() ? 0 : enrollmentRepository.countByCourseIdIn(courseIds);

        // Danh sách học kỳ đang có và số lượng học kỳ
        List<String> activeSemesters = courses.stream()
                .map(Course::getSemester)
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        int activeSemestersCount = activeSemesters.size();

        // Tổng số buổi điểm danh thuộc các khóa học
        long totalSessions = courseIds.isEmpty() ? 0 : attendanceSessionRepository.countByCourseIdIn(courseIds);

        // Danh sách các khóa học gần đây nhất (tối đa 5 khóa học sắp xếp theo ngày tạo giảm dần)
        List<RecentCourseResponse> recentCourses = courses.stream()
                .sorted((c1, c2) -> {
                    if (c1.getCreatedAt() == null && c2.getCreatedAt() == null) return 0;
                    if (c1.getCreatedAt() == null) return 1;
                    if (c2.getCreatedAt() == null) return -1;
                    return c2.getCreatedAt().compareTo(c1.getCreatedAt());
                })
                .limit(5)
                .map(c -> RecentCourseResponse.builder()
                        .courseId(c.getCourseId())
                        .subjectCode(c.getSubjectCode())
                        .className(c.getClassName())
                        .semester(c.getSemester())
                        .studentCount(enrollmentRepository.countByCourseId(c.getCourseId()))
                        .build())
                .collect(Collectors.toList());

        log.info("📊 Thống kê Dashboard giảng viên: lecturerId={}, courses={}, students={}, semesters={}, sessions={}",
                targetLecturerId, totalCourses, totalRegisteredStudents, activeSemestersCount, totalSessions);

        return LecturerDashboardStatsResponse.builder()
                .lecturerId(targetLecturerId)
                .totalCourses(totalCourses)
                .totalRegisteredStudents(totalRegisteredStudents)
                .activeSemestersCount(activeSemestersCount)
                .activeSemesters(activeSemesters)
                .totalSessions(totalSessions)
                .recentCourses(recentCourses)
                .build();
    }
}
