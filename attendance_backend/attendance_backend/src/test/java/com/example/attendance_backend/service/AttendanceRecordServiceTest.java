package com.example.attendance_backend.service;

import com.example.attendance_backend.dto.request.CheckInRequest;
import com.example.attendance_backend.dto.response.AttendanceRecordResponse;
import com.example.attendance_backend.exception.AppException;
import com.example.attendance_backend.model.AttendanceRecord;
import com.example.attendance_backend.model.AttendanceSession;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.AttendanceRecordRepository;
import com.example.attendance_backend.repository.AttendanceSessionRepository;
import com.example.attendance_backend.repository.EnrollmentRepository;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.service.impl.AttendanceRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceRecordServiceTest {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private AttendanceSessionRepository attendanceSessionRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SyncLogService syncLogService;

    private AttendanceRecordService attendanceRecordService;

    @BeforeEach
    void setUp() {
        attendanceRecordService = new AttendanceRecordServiceImpl(
                attendanceRecordRepository,
                attendanceSessionRepository,
                enrollmentRepository,
                userRepository,
                syncLogService
        );
    }

    @Test
    void testCheckInSuccess() throws IOException {
        CheckInRequest request = CheckInRequest.builder()
                .sessionId("SES-001")
                .qrCodeSecret("SECRET123")
                .build();

        User student = User.builder()
                .userId("USR-STU1")
                .username("student1")
                .fapCode("SE192964")
                .fullName("Nguyen Van A")
                .roleId("ROLE_STUDENT")
                .build();

        AttendanceSession session = AttendanceSession.builder()
                .sessionId("SES-001")
                .courseId("CRS-101")
                .qrCodeSecret("SECRET123")
                .status("ACTIVE")
                .expiredAt("2099-12-31 23:59:59")
                .build();

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(attendanceSessionRepository.findById("SES-001")).thenReturn(Optional.of(session));
        when(enrollmentRepository.existsByCourseIdAndStudentId("CRS-101", "USR-STU1")).thenReturn(true);
        when(attendanceRecordRepository.existsBySessionIdAndStudentId("SES-001", "USR-STU1")).thenReturn(false);
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceRecordResponse response = attendanceRecordService.checkIn(request, "student1", "127.0.0.1");

        assertNotNull(response);
        assertEquals("PRESENT", response.getStatus());
        assertEquals("SE192964", response.getStudentFapCode());
        verify(attendanceRecordRepository, times(1)).save(any(AttendanceRecord.class));
    }

    @Test
    void testCheckInNotEnrolledThrowsException() throws IOException {
        CheckInRequest request = CheckInRequest.builder()
                .sessionId("SES-001")
                .qrCodeSecret("SECRET123")
                .build();

        User student = User.builder()
                .userId("USR-STU1")
                .username("student1")
                .roleId("ROLE_STUDENT")
                .build();

        AttendanceSession session = AttendanceSession.builder()
                .sessionId("SES-001")
                .courseId("CRS-101")
                .qrCodeSecret("SECRET123")
                .status("ACTIVE")
                .expiredAt("2099-12-31 23:59:59")
                .build();

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(attendanceSessionRepository.findById("SES-001")).thenReturn(Optional.of(session));
        when(enrollmentRepository.existsByCourseIdAndStudentId("CRS-101", "USR-STU1")).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> attendanceRecordService.checkIn(request, "student1", "127.0.0.1"));
        assertTrue(ex.getMessage().contains("chưa đăng ký tham gia"));
        verify(attendanceRecordRepository, never()).save(any());
    }

    @Test
    void testCheckInAlreadyCheckedInThrowsException() throws IOException {
        CheckInRequest request = CheckInRequest.builder()
                .sessionId("SES-001")
                .qrCodeSecret("SECRET123")
                .build();

        User student = User.builder()
                .userId("USR-STU1")
                .username("student1")
                .roleId("ROLE_STUDENT")
                .build();

        AttendanceSession session = AttendanceSession.builder()
                .sessionId("SES-001")
                .courseId("CRS-101")
                .qrCodeSecret("SECRET123")
                .status("ACTIVE")
                .expiredAt("2099-12-31 23:59:59")
                .build();

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(attendanceSessionRepository.findById("SES-001")).thenReturn(Optional.of(session));
        when(enrollmentRepository.existsByCourseIdAndStudentId("CRS-101", "USR-STU1")).thenReturn(true);
        when(attendanceRecordRepository.existsBySessionIdAndStudentId("SES-001", "USR-STU1")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> attendanceRecordService.checkIn(request, "student1", "127.0.0.1"));
        assertTrue(ex.getMessage().contains("đã thực hiện điểm danh"));
        verify(attendanceRecordRepository, never()).save(any());
    }
}
