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

    private CourseService courseService;

    private User lecturer;
    private User otherLecturer;
    private User student;
    private Course sampleCourse;

    @BeforeEach
    void setUp() {
        courseService = new CourseServiceImpl(courseRepository, userRepository);

        lecturer = User.builder()
                .userId("USR-LEC1")
                .username("teacher1")
                .roleId("ROLE_LECTURER")
                .build();

        otherLecturer = User.builder()
                .userId("USR-LEC2")
                .username("teacher2")
                .roleId("ROLE_LECTURER")
                .build();

        student = User.builder()
                .userId("USR-STU1")
                .username("student1")
                .roleId("ROLE_STUDENT")
                .build();

        sampleCourse = Course.builder()
                .courseId("CRS-001")
                .subjectCode("PRM392")
                .className("SE1701")
                .semester("FA24")
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
}
