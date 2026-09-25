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
import java.time.LocalDateTime;
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
                .expiredAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
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
                .expiredAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
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
                .expiredAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build();

        when(userRepository.findByUsername("student1")).thenReturn(Optional.of(student));
        when(attendanceSessionRepository.findById("SES-001")).thenReturn(Optional.of(session));
        when(enrollmentRepository.existsByCourseIdAndStudentId("CRS-101", "USR-STU1")).thenReturn(true);
        when(attendanceRecordRepository.existsBySessionIdAndStudentId("SES-001", "USR-STU1")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> attendanceRecordService.checkIn(request, "student1", "127.0.0.1"));
        assertTrue(ex.getMessage().contains("đã thực hiện điểm danh"));
        verify(attendanceRecordRepository, never()).save(any());
    }

    @Test
    void testCheckInWithDynamicTotpSuccess() throws IOException {
        String sessionSecret = "TOTP_SECRET_XYZ_123456";
        String validTotpToken = com.example.attendance_backend.util.TotpUtil.generateCurrentToken(sessionSecret);

        CheckInRequest request = CheckInRequest.builder()
                .sessionId("SES-002")
                .qrToken(validTotpToken)
                .build();

        User student = User.builder()
                .userId("USR-STU2")
                .username("student2")
                .fapCode("SE192965")
                .fullName("Tran Van B")
                .roleId("ROLE_STUDENT")
                .build();

        AttendanceSession session = AttendanceSession.builder()
                .sessionId("SES-002")
                .courseId("CRS-101")
                .qrCodeSecret(sessionSecret)
                .status("ACTIVE")
                .expiredAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build();

        when(userRepository.findByUsername("student2")).thenReturn(Optional.of(student));
        when(attendanceSessionRepository.findById("SES-002")).thenReturn(Optional.of(session));
        when(enrollmentRepository.existsByCourseIdAndStudentId("CRS-101", "USR-STU2")).thenReturn(true);
        when(attendanceRecordRepository.existsBySessionIdAndStudentId("SES-002", "USR-STU2")).thenReturn(false);
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceRecordResponse response = attendanceRecordService.checkIn(request, "student2", "127.0.0.1");

        assertNotNull(response);
        assertEquals("PRESENT", response.getStatus());
        assertEquals("SE192965", response.getStudentFapCode());
    }

    @Test
    void testCheckInWithInvalidTotpThrowsException() throws IOException {
        CheckInRequest request = CheckInRequest.builder()
                .sessionId("SES-003")
                .qrToken("INVALID_TOKEN_999")
                .build();

        User student = User.builder()
                .userId("USR-STU3")
                .username("student3")
                .roleId("ROLE_STUDENT")
                .build();

        AttendanceSession session = AttendanceSession.builder()
                .sessionId("SES-003")
                .courseId("CRS-101")
                .qrCodeSecret("REAL_SECRET_123")
                .status("ACTIVE")
                .expiredAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build();

        when(userRepository.findByUsername("student3")).thenReturn(Optional.of(student));
        when(attendanceSessionRepository.findById("SES-003")).thenReturn(Optional.of(session));

        AppException ex = assertThrows(AppException.class, () -> attendanceRecordService.checkIn(request, "student3", "127.0.0.1"));
        assertTrue(ex.getMessage().contains("Mã QR không hợp lệ"));
    }

    @Test
    void testCheckInWithBssidMatchSuccess() throws IOException {
        String sessionSecret = "SECRET_BSSID_123";
        String validTotp = com.example.attendance_backend.util.TotpUtil.generateCurrentToken(sessionSecret);

        CheckInRequest request = CheckInRequest.builder()
                .sessionId("SES-004")
                .qrToken(validTotp)
                .bssid("c8:0e:14:2b:8a:90")
                .build();

        User student = User.builder()
                .userId("USR-STU4")
                .username("student4")
                .fapCode("SE192966")
                .roleId("ROLE_STUDENT")
                .build();

        AttendanceSession session = AttendanceSession.builder()
                .sessionId("SES-004")
                .courseId("CRS-101")
                .qrCodeSecret(sessionSecret)
                .roomBssid("c8:0e:14:2b:8a:90")
                .status("ACTIVE")
                .expiredAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build();

        when(userRepository.findByUsername("student4")).thenReturn(Optional.of(student));
        when(attendanceSessionRepository.findById("SES-004")).thenReturn(Optional.of(session));
        when(enrollmentRepository.existsByCourseIdAndStudentId("CRS-101", "USR-STU4")).thenReturn(true);
        when(attendanceRecordRepository.existsBySessionIdAndStudentId("SES-004", "USR-STU4")).thenReturn(false);
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceRecordResponse response = attendanceRecordService.checkIn(request, "student4", "10.0.0.1");
        assertNotNull(response);
        assertEquals("PRESENT", response.getStatus());
    }

    @Test
    void testCheckInWithBssidMismatchThrowsException() throws IOException {
        String sessionSecret = "SECRET_BSSID_123";
        String validTotp = com.example.attendance_backend.util.TotpUtil.generateCurrentToken(sessionSecret);

        CheckInRequest request = CheckInRequest.builder()
                .sessionId("SES-005")
                .qrToken(validTotp)
                .bssid("aa:bb:cc:dd:ee:ff") // Mismatched BSSID
                .build();

        User student = User.builder()
                .userId("USR-STU5")
                .username("student5")
                .roleId("ROLE_STUDENT")
                .build();

        AttendanceSession session = AttendanceSession.builder()
                .sessionId("SES-005")
                .courseId("CRS-101")
                .qrCodeSecret(sessionSecret)
                .roomBssid("c8:0e:14:2b:8a:90")
                .status("ACTIVE")
                .expiredAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build();

        when(userRepository.findByUsername("student5")).thenReturn(Optional.of(student));
        when(attendanceSessionRepository.findById("SES-005")).thenReturn(Optional.of(session));
        when(enrollmentRepository.existsByCourseIdAndStudentId("CRS-101", "USR-STU5")).thenReturn(true);
        when(attendanceRecordRepository.existsBySessionIdAndStudentId("SES-005", "USR-STU5")).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> attendanceRecordService.checkIn(request, "student5", "10.0.0.1"));
        assertTrue(ex.getMessage().contains("BSSID không khớp"));
    }

    @Test
    void testCheckInDeviceAlreadyUsedThrowsException() throws IOException {
        String sessionSecret = "SECRET_DEV_123";
        String validTotp = com.example.attendance_backend.util.TotpUtil.generateCurrentToken(sessionSecret);

        CheckInRequest request = CheckInRequest.builder()
                .sessionId("SES-006")
                .qrToken(validTotp)
                .deviceId("DEVICE-MAC-UUID-999")
                .build();

        User student = User.builder()
                .userId("USR-STU6")
                .username("student6")
                .roleId("ROLE_STUDENT")
                .build();

        AttendanceSession session = AttendanceSession.builder()
                .sessionId("SES-006")
                .courseId("CRS-101")
                .qrCodeSecret(sessionSecret)
                .status("ACTIVE")
                .expiredAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build();

        when(userRepository.findByUsername("student6")).thenReturn(Optional.of(student));
        when(attendanceSessionRepository.findById("SES-006")).thenReturn(Optional.of(session));
        when(enrollmentRepository.existsByCourseIdAndStudentId("CRS-101", "USR-STU6")).thenReturn(true);
        when(attendanceRecordRepository.existsBySessionIdAndStudentId("SES-006", "USR-STU6")).thenReturn(false);
        // Giả lập thiết bị này đã được sinh viên khác điểm danh trong session này
        when(attendanceRecordRepository.existsBySessionIdAndDeviceId("SES-006", "DEVICE-MAC-UUID-999")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> attendanceRecordService.checkIn(request, "student6", "127.0.0.1"));
        assertTrue(ex.getMessage().contains("Thiết bị này đã được sử dụng"));
    }
}
