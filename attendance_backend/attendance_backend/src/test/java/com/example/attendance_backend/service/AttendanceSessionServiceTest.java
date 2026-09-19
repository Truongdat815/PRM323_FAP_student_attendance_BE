package com.example.attendance_backend.service;

import com.example.attendance_backend.dto.request.SessionCreateRequest;
import com.example.attendance_backend.dto.response.SessionResponse;
import com.example.attendance_backend.exception.AppException;
import com.example.attendance_backend.model.AttendanceSession;
import com.example.attendance_backend.model.Course;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.AttendanceSessionRepository;
import com.example.attendance_backend.repository.CourseRepository;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.service.impl.AttendanceSessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceSessionServiceTest {

    @Mock
    private AttendanceSessionRepository attendanceSessionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SyncLogService syncLogService;

    private AttendanceSessionService attendanceSessionService;

    @BeforeEach
    void setUp() {
        attendanceSessionService = new AttendanceSessionServiceImpl(
                attendanceSessionRepository,
                courseRepository,
                userRepository,
                syncLogService
        );
    }

    @Test
    void testCreateSessionSuccess() throws IOException {
        SessionCreateRequest request = SessionCreateRequest.builder()
                .courseId("CRS-101")
                .slotNumber(1)
                .expiredAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .roomNetworkIp("192.168.1.1")
                .build();

        User lecturer = User.builder()
                .userId("USR-LEC1")
                .username("lecturer1")
                .roleId("ROLE_LECTURER")
                .build();

        Course course = Course.builder()
                .courseId("CRS-101")
                .lecturerId("USR-LEC1")
                .subjectCode("PRM392")
                .className("SE1801")
                .build();

        when(userRepository.findByUsername("lecturer1")).thenReturn(Optional.of(lecturer));
        when(courseRepository.findById("CRS-101")).thenReturn(Optional.of(course));
        when(attendanceSessionRepository.save(any(AttendanceSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessionResponse response = attendanceSessionService.createSession(request, "lecturer1");

        assertNotNull(response);
        assertEquals("CRS-101", response.getCourseId());
        assertEquals(1, response.getSlotNumber());
        assertEquals("ACTIVE", response.getStatus());
        assertNotNull(response.getQrCodeSecret());
        verify(attendanceSessionRepository, times(1)).save(any(AttendanceSession.class));
        verify(syncLogService, times(1)).logSync(anyString(), anyString(), anyString());
    }

    @Test
    void testCreateSessionStudentRoleForbidden() throws IOException {
        SessionCreateRequest request = SessionCreateRequest.builder()
                .courseId("CRS-101")
                .slotNumber(1)
                .expiredAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build();

        User student = User.builder()
                .userId("USR-STU1")
                .username("student1")
                .roleId("ROLE_STUDENT")
                .build();

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));

        AppException ex = assertThrows(AppException.class, () -> attendanceSessionService.createSession(request, "student1"));
        assertTrue(ex.getMessage().contains("mới được tạo phiên điểm danh"));
        verify(attendanceSessionRepository, never()).save(any());
    }
}
