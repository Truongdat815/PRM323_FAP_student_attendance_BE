package com.example.attendance_backend.service;

import com.example.attendance_backend.dto.request.CourseRequest;
import com.example.attendance_backend.dto.response.CourseResponse;
import com.example.attendance_backend.exception.AppException;
import com.example.attendance_backend.model.Course;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.CourseRepository;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.service.impl.CourseServiceImpl;
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
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.example.attendance_backend.repository.EnrollmentRepository enrollmentRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private CourseService courseService;

    private User lecturer;
    private User otherLecturer;
    private User student;
    private Course sampleCourse;

    @BeforeEach
    void setUp() {
        courseService = new CourseServiceImpl(courseRepository, userRepository, enrollmentRepository, passwordEncoder);

        lecturer = User.builder()
                .userId("USR-LEC1")
                .username("teacher1")
                .fullName("Nguyễn Văn A")
                .roleId("ROLE_LECTURER")
                .build();

        otherLecturer = User.builder()
                .userId("USR-LEC2")
                .username("teacher2")
                .fullName("Trần Thị B")
                .roleId("ROLE_LECTURER")
                .build();

        student = User.builder()
                .userId("USR-STU1")
                .username("student1")
                .fullName("Lê Văn C")
                .roleId("ROLE_STUDENT")
                .build();

        sampleCourse = Course.builder()
                .courseId("CRS-001")
                .subjectCode("PRM392")
                .className("SE1701")
                .semester("FA24")
                .password("secret123")
                .lecturerId("USR-LEC1")
                .createdAt(LocalDateTime.of(2026, 9, 16, 0, 0, 0))
                .build();
    }

    @Test
    void testGetAllCourses() throws IOException {
        when(courseRepository.findAll()).thenReturn(Arrays.asList(sampleCourse));

        List<CourseResponse> responses = courseService.getAllCourses(null);

        assertEquals(1, responses.size());
        assertEquals("CRS-001", responses.get(0).getCourseId());
        assertEquals("PRM392", responses.get(0).getSubjectCode());
    }

    @Test
    void testGetCourseByIdSuccess() throws IOException {
        when(courseRepository.findById("CRS-001")).thenReturn(Optional.of(sampleCourse));

        CourseResponse response = courseService.getCourseById("CRS-001");

        assertNotNull(response);
        assertEquals("PRM392", response.getSubjectCode());
        assertEquals("SE1701", response.getClassName());
    }

    @Test
    void testGetCourseByIdNotFoundThrowsException() throws IOException {
        when(courseRepository.findById("CRS-NONEXIST")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> courseService.getCourseById("CRS-NONEXIST"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void testCreateCourseSuccess() throws IOException {
        CourseRequest request = CourseRequest.builder()
                .subjectCode("SWP391")
                .className("SE1702")
                .semester("SP25")
                .build();

        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(lecturer));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseResponse response = courseService.createCourse(request, "teacher1");

        assertNotNull(response);
        assertTrue(response.getCourseId().startsWith("CRS-"));
        assertEquals("SWP391", response.getSubjectCode());
        assertEquals("SE1702", response.getClassName());
        assertEquals("USR-LEC1", response.getLecturerId());
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void testCreateCourseForbiddenForStudent() throws IOException {
        CourseRequest request = CourseRequest.builder()
                .subjectCode("SWP391")
                .className("SE1702")
                .semester("SP25")
                .build();

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));

        AppException ex = assertThrows(AppException.class, () -> courseService.createCourse(request, "student1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testUpdateCourseSuccess() throws IOException {
        CourseRequest updateRequest = CourseRequest.builder()
                .subjectCode("PRM392-UPDATED")
                .className("SE1701-NEW")
                .semester("SU25")
                .build();

        when(courseRepository.findById("CRS-001")).thenReturn(Optional.of(sampleCourse));
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(lecturer));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseResponse response = courseService.updateCourse("CRS-001", updateRequest, "teacher1");

        assertNotNull(response);
        assertEquals("PRM392-UPDATED", response.getSubjectCode());
        assertEquals("SE1701-NEW", response.getClassName());
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void testUpdateCourseForbiddenForOtherLecturer() throws IOException {
        CourseRequest updateRequest = CourseRequest.builder()
                .subjectCode("PRM392")
                .className("SE1701")
                .semester("FA24")
                .build();

        when(courseRepository.findById("CRS-001")).thenReturn(Optional.of(sampleCourse));
        when(userRepository.findByUsername("teacher2")).thenReturn(Optional.of(otherLecturer));

        AppException ex = assertThrows(AppException.class, () -> courseService.updateCourse("CRS-001", updateRequest, "teacher2"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void testDeleteCourseSuccess() throws IOException {
        when(courseRepository.findById("CRS-001")).thenReturn(Optional.of(sampleCourse));
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(lecturer));

        assertDoesNotThrow(() -> courseService.deleteCourse("CRS-001", "teacher1"));
        verify(courseRepository, times(1)).delete(any(Course.class));
    }

    @Test
    void testDeleteCourseForbiddenForOtherLecturer() throws IOException {
        when(courseRepository.findById("CRS-001")).thenReturn(Optional.of(sampleCourse));
        when(userRepository.findByUsername("teacher2")).thenReturn(Optional.of(otherLecturer));

        AppException ex = assertThrows(AppException.class, () -> courseService.deleteCourse("CRS-001", "teacher2"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(courseRepository, never()).delete(any(Course.class));
    }

    @Test
    void testRegisterCourseSuccess_WithPassword() throws IOException {
        com.example.attendance_backend.dto.request.CourseRegisterRequest request =
                com.example.attendance_backend.dto.request.CourseRegisterRequest.builder()
                        .courseId("CRS-001")
                        .userId("USR-STU1")
                        .coursePassword("secret123")
                        .build();

        when(userRepository.findById("USR-STU1")).thenReturn(Optional.of(student));
        when(courseRepository.findById("CRS-001")).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.existsByCourseIdAndStudentId("CRS-001", "USR-STU1")).thenReturn(false);
        when(userRepository.findById("USR-LEC1")).thenReturn(Optional.of(lecturer));

        CourseResponse response = courseService.registerCourse(request, "student1");

        assertNotNull(response);
        assertEquals("CRS-001", response.getCourseId());
        assertEquals("PRM392", response.getSubjectCode());
        assertEquals("Nguyễn Văn A", response.getLecturerName());
        verify(enrollmentRepository, times(1)).save(any(com.example.attendance_backend.model.Enrollment.class));
    }

    @Test
    void testRegisterCourseWrongPassword() throws IOException {
        com.example.attendance_backend.dto.request.CourseRegisterRequest request =
                com.example.attendance_backend.dto.request.CourseRegisterRequest.builder()
                        .courseId("CRS-001")
                        .userId("USR-STU1")
                        .coursePassword("wrongpassword")
                        .build();

        when(userRepository.findById("USR-STU1")).thenReturn(Optional.of(student));
        when(courseRepository.findById("CRS-001")).thenReturn(Optional.of(sampleCourse));

        AppException ex = assertThrows(AppException.class, () -> courseService.registerCourse(request, "student1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void testRegisterCourseAlreadyEnrolled() throws IOException {
        com.example.attendance_backend.dto.request.CourseRegisterRequest request =
                com.example.attendance_backend.dto.request.CourseRegisterRequest.builder()
                        .courseId("CRS-001")
                        .userId("USR-STU1")
                        .coursePassword("secret123")
                        .build();

        when(userRepository.findById("USR-STU1")).thenReturn(Optional.of(student));
        when(courseRepository.findById("CRS-001")).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.existsByCourseIdAndStudentId("CRS-001", "USR-STU1")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> courseService.registerCourse(request, "student1"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void testSearchCoursesSuccess() throws IOException {
        when(courseRepository.searchCourses("SE1701", "PRM392", "FA24", "Nguyễn", null))
                .thenReturn(List.of(sampleCourse));
        when(userRepository.findAllById(any())).thenReturn(List.of(lecturer));

        List<CourseResponse> responses = courseService.searchCourses("SE1701", "PRM392", "FA24", "Nguyễn");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("CRS-001", responses.get(0).getCourseId());
        assertEquals("Nguyễn Văn A", responses.get(0).getLecturerName());
    }

    @Test
    void testGetLecturerCourses() throws IOException {
        when(userRepository.findById("USR-LEC1")).thenReturn(Optional.of(lecturer));
        when(courseRepository.findByLecturerId("USR-LEC1")).thenReturn(List.of(sampleCourse));

        List<CourseResponse> responses = courseService.getLecturerCourses("USR-LEC1", "PRM392", "FA24");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("CRS-001", responses.get(0).getCourseId());
        assertEquals("Nguyễn Văn A", responses.get(0).getLecturerName());
    }

    @Test
    void testGetMyCoursesAsLecturer() throws IOException {
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(lecturer));
        when(userRepository.findById("USR-LEC1")).thenReturn(Optional.of(lecturer));
        when(courseRepository.findByLecturerId("USR-LEC1")).thenReturn(List.of(sampleCourse));

        List<CourseResponse> responses = courseService.getMyCourses("teacher1", null, null, null, null);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("CRS-001", responses.get(0).getCourseId());
        assertEquals("Nguyễn Văn A", responses.get(0).getLecturerName());
    }

    @Test
    void testGetMyCoursesAsStudent() throws IOException {
        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(userRepository.findById("USR-STU1")).thenReturn(Optional.of(student));
        com.example.attendance_backend.model.Enrollment enrollment = com.example.attendance_backend.model.Enrollment.builder()
                .enrollmentId("ENR-1")
                .courseId("CRS-001")
                .studentId("USR-STU1")
                .build();
        when(enrollmentRepository.findByStudentId("USR-STU1")).thenReturn(List.of(enrollment));
        when(courseRepository.findAllById(List.of("CRS-001"))).thenReturn(List.of(sampleCourse));
        when(userRepository.findAllById(any())).thenReturn(List.of(lecturer));

        List<CourseResponse> responses = courseService.getMyCourses("student1", null, null, null, null);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("CRS-001", responses.get(0).getCourseId());
    }
}
