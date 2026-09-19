package com.example.attendance_backend.service;

import com.example.attendance_backend.dto.request.EnrollmentRequest;
import com.example.attendance_backend.dto.response.EnrollmentResponse;
import com.example.attendance_backend.exception.AppException;
import com.example.attendance_backend.model.Course;
import com.example.attendance_backend.model.Enrollment;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.CourseRepository;
import com.example.attendance_backend.repository.EnrollmentRepository;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.service.impl.EnrollmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    private EnrollmentService enrollmentService;

    private User student;
    private User lecturer;
    private Course sampleCourse;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentServiceImpl(enrollmentRepository, courseRepository, userRepository);

        student = User.builder()
                .userId("USR-STU1")
                .username("student1")
                .fullName("Nguyen Van A")
                .fapCode("SE192964")
                .roleId("ROLE_STUDENT")
                .build();

        lecturer = User.builder()
                .userId("USR-LEC1")
                .username("teacher1")
                .fullName("Teacher One")
                .roleId("ROLE_LECTURER")
                .build();

        sampleCourse = Course.builder()
                .courseId("CRS-001")
                .subjectCode("PRM392")
                .className("SE1701")
                .semester("FA24")
                .lecturerId("USR-LEC1")
                .build();
    }

    @Test
    void testEnrollCourseSuccess() throws IOException {
        EnrollmentRequest request = EnrollmentRequest.builder()
                .courseId("CRS-001")
                .build();

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(courseRepository.findById("CRS-001")).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.existsByCourseIdAndStudentId("CRS-001", "USR-STU1")).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EnrollmentResponse response = enrollmentService.enrollCourse(request, "student1");

        assertNotNull(response);
        assertTrue(response.getEnrollmentId().startsWith("ENR-"));
        assertEquals("CRS-001", response.getCourseId());
        assertEquals("USR-STU1", response.getStudentId());
        assertEquals("PRM392", response.getSubjectCode());
        assertEquals("SE192964", response.getFapCode());
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    @Test
    void testEnrollCourseNotFoundThrowsException() throws IOException {
        EnrollmentRequest request = EnrollmentRequest.builder()
                .courseId("CRS-NONEXIST")
                .build();

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(courseRepository.findById("CRS-NONEXIST")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> enrollmentService.enrollCourse(request, "student1"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void testEnrollCourseDuplicateThrowsException() throws IOException {
        EnrollmentRequest request = EnrollmentRequest.builder()
                .courseId("CRS-001")
                .build();

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(courseRepository.findById("CRS-001")).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.existsByCourseIdAndStudentId("CRS-001", "USR-STU1")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> enrollmentService.enrollCourse(request, "student1"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("đã đăng ký tham gia lớp học phần này trước đó"));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void testEnrollCourseForbiddenForLecturer() throws IOException {
        EnrollmentRequest request = EnrollmentRequest.builder()
                .courseId("CRS-001")
                .build();

        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(lecturer));

        AppException ex = assertThrows(AppException.class, () -> enrollmentService.enrollCourse(request, "teacher1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void testGetMyEnrollmentsSuccess() throws IOException {
        Enrollment enrollment = Enrollment.builder()
                .enrollmentId("ENR-001")
                .courseId("CRS-001")
                .studentId("USR-STU1")
                .enrolledAt(LocalDateTime.of(2026, 9, 16, 0, 0, 0))
                .build();

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentId("USR-STU1")).thenReturn(Arrays.asList(enrollment));
        when(courseRepository.findById("CRS-001")).thenReturn(Optional.of(sampleCourse));

        List<EnrollmentResponse> responses = enrollmentService.getMyEnrollments("student1");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("PRM392", responses.get(0).getSubjectCode());
        assertEquals("SE192964", responses.get(0).getFapCode());
    }
}
