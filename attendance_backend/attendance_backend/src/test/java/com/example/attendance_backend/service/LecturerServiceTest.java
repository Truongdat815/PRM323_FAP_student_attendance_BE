package com.example.attendance_backend.service;

import com.example.attendance_backend.dto.response.LecturerDashboardStatsResponse;
import com.example.attendance_backend.model.Course;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.AttendanceSessionRepository;
import com.example.attendance_backend.repository.CourseRepository;
import com.example.attendance_backend.repository.EnrollmentRepository;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.service.impl.LecturerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LecturerServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private AttendanceSessionRepository attendanceSessionRepository;

    private LecturerService lecturerService;

    private User lecturer;
    private User admin;
    private Course course1;
    private Course course2;

    @BeforeEach
    void setUp() {
        lecturerService = new LecturerServiceImpl(userRepository, courseRepository, enrollmentRepository, attendanceSessionRepository);

        lecturer = User.builder()
                .userId("USR-LEC1")
                .username("teacher1")
                .fullName("Nguyễn Văn A")
                .roleId("ROLE_LECTURER")
                .build();

        admin = User.builder()
                .userId("USR-ADM1")
                .username("admin1")
                .fullName("Quản trị viên")
                .roleId("ROLE_ADMIN")
                .build();

        course1 = Course.builder()
                .courseId("CRS-001")
                .subjectCode("PRM392")
                .className("SE1701")
                .semester("FA24")
                .lecturerId("USR-LEC1")
                .createdAt(LocalDateTime.of(2026, 9, 16, 10, 0))
                .build();

        course2 = Course.builder()
                .courseId("CRS-002")
                .subjectCode("SWP391")
                .className("SE1702")
                .semester("SP25")
                .lecturerId("USR-LEC1")
                .createdAt(LocalDateTime.of(2026, 9, 18, 10, 0))
                .build();
    }

    @Test
    void testGetDashboardStatsAsLecturerSuccess() throws IOException {
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(lecturer));
        when(courseRepository.findByLecturerId("USR-LEC1")).thenReturn(List.of(course1, course2));
        when(enrollmentRepository.countByCourseIdIn(List.of("CRS-001", "CRS-002"))).thenReturn(50L);
        when(attendanceSessionRepository.countByCourseIdIn(List.of("CRS-001", "CRS-002"))).thenReturn(12L);
        when(enrollmentRepository.countByCourseId("CRS-001")).thenReturn(25L);
        when(enrollmentRepository.countByCourseId("CRS-002")).thenReturn(25L);

        LecturerDashboardStatsResponse stats = lecturerService.getDashboardStats("teacher1");

        assertNotNull(stats);
        assertEquals("USR-LEC1", stats.getLecturerId());
        assertEquals(2, stats.getTotalCourses());
        assertEquals(50L, stats.getTotalRegisteredStudents());
        assertEquals(2, stats.getActiveSemestersCount());
        assertTrue(stats.getActiveSemesters().contains("FA24"));
        assertTrue(stats.getActiveSemesters().contains("SP25"));
        assertEquals(12L, stats.getTotalSessions());
        assertEquals(2, stats.getRecentCourses().size());
        // Course 2 is more recent
        assertEquals("CRS-002", stats.getRecentCourses().get(0).getCourseId());
        assertEquals(25L, stats.getRecentCourses().get(0).getStudentCount());
    }

    @Test
    void testGetDashboardStatsAsAdminWithLecturerId() throws IOException {
        when(userRepository.findByUsername("admin1")).thenReturn(Optional.of(admin));
        when(courseRepository.findByLecturerId("USR-LEC1")).thenReturn(List.of(course1));
        when(enrollmentRepository.countByCourseIdIn(List.of("CRS-001"))).thenReturn(30L);
        when(attendanceSessionRepository.countByCourseIdIn(List.of("CRS-001"))).thenReturn(8L);
        when(enrollmentRepository.countByCourseId("CRS-001")).thenReturn(30L);

        LecturerDashboardStatsResponse stats = lecturerService.getDashboardStats("admin1", "USR-LEC1");

        assertNotNull(stats);
        assertEquals("USR-LEC1", stats.getLecturerId());
        assertEquals(1, stats.getTotalCourses());
        assertEquals(30L, stats.getTotalRegisteredStudents());
        assertEquals(1, stats.getActiveSemestersCount());
        assertEquals(List.of("FA24"), stats.getActiveSemesters());
        assertEquals(8L, stats.getTotalSessions());
        assertEquals(1, stats.getRecentCourses().size());
    }

    @Test
    void testGetDashboardStatsEmptyCourses() throws IOException {
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(lecturer));
        when(courseRepository.findByLecturerId("USR-LEC1")).thenReturn(Collections.emptyList());

        LecturerDashboardStatsResponse stats = lecturerService.getDashboardStats("teacher1");

        assertNotNull(stats);
        assertEquals("USR-LEC1", stats.getLecturerId());
        assertEquals(0, stats.getTotalCourses());
        assertEquals(0L, stats.getTotalRegisteredStudents());
        assertEquals(0, stats.getActiveSemestersCount());
        assertTrue(stats.getActiveSemesters().isEmpty());
        assertEquals(0L, stats.getTotalSessions());
        assertTrue(stats.getRecentCourses().isEmpty());
    }
}
