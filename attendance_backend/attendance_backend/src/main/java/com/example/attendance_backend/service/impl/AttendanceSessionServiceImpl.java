package com.example.attendance_backend.service.impl;

import com.example.attendance_backend.dto.request.SessionCreateRequest;
import com.example.attendance_backend.dto.response.SessionResponse;
import com.example.attendance_backend.exception.AppException;
import com.example.attendance_backend.model.AttendanceSession;
import com.example.attendance_backend.model.Course;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.AttendanceSessionRepository;
import com.example.attendance_backend.repository.CourseRepository;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.service.AttendanceSessionService;
import com.example.attendance_backend.service.SyncLogService;
import com.example.attendance_backend.util.QRCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AttendanceSessionServiceImpl implements AttendanceSessionService {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final SyncLogService syncLogService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public AttendanceSessionServiceImpl(AttendanceSessionRepository attendanceSessionRepository,
                                        CourseRepository courseRepository,
                                        UserRepository userRepository,
                                        SyncLogService syncLogService) {
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.syncLogService = syncLogService;
    }

    @Override
    public SessionResponse createSession(SessionCreateRequest request, String currentUsername) throws IOException {
        User currentUser = getCurrentUser(currentUsername);

        if (!"ROLE_LECTURER".equalsIgnoreCase(currentUser.getRoleId()) && !"ROLE_ADMIN".equalsIgnoreCase(currentUser.getRoleId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Chỉ giảng viên hoặc admin mới được tạo phiên điểm danh.");
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với mã: " + request.getCourseId()));

        if (!currentUser.getUserId().equalsIgnoreCase(course.getLecturerId()) && !"ROLE_ADMIN".equalsIgnoreCase(currentUser.getRoleId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền tạo buổi điểm danh cho khóa học do giảng viên khác phụ trách.");
        }

        String sessionId = "SES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String qrSecret = QRCodeGenerator.generateSecret();
        String qrBase64 = QRCodeGenerator.generateQRCodeBase64(qrSecret, 250, 250);
        String now = LocalDateTime.now().format(FORMATTER);

        AttendanceSession session = AttendanceSession.builder()
                .sessionId(sessionId)
                .courseId(course.getCourseId())
                .slotNumber(request.getSlotNumber())
                .qrCodeSecret(qrSecret)
                .roomNetworkIp(request.getRoomNetworkIp() != null ? request.getRoomNetworkIp().trim() : "")
                .expiredAt(request.getExpiredAt().trim())
                .status("ACTIVE")
                .createdAt(now)
                .build();

        attendanceSessionRepository.save(session);
        syncLogService.logSync("CREATE_SESSION", "SUCCESS", "Created session " + sessionId + " for course " + course.getCourseId());

        return mapToResponse(session, qrBase64);
    }

    @Override
    public SessionResponse getSessionById(String sessionId) throws IOException {
        AttendanceSession session = attendanceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy buổi điểm danh với mã: " + sessionId));
        String qrBase64 = QRCodeGenerator.generateQRCodeBase64(session.getQrCodeSecret(), 250, 250);
        return mapToResponse(session, qrBase64);
    }

    @Override
    public List<SessionResponse> getSessionsByCourse(String courseId) throws IOException {
        List<AttendanceSession> sessions = attendanceSessionRepository.findByCourseId(courseId);
        return sessions.stream()
                .map(session -> mapToResponse(session, QRCodeGenerator.generateQRCodeBase64(session.getQrCodeSecret(), 250, 250)))
                .collect(Collectors.toList());
    }

    @Override
    public SessionResponse closeSession(String sessionId, String currentUsername) throws IOException {
        AttendanceSession session = attendanceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy buổi điểm danh với mã: " + sessionId));

        User currentUser = getCurrentUser(currentUsername);

        Course course = courseRepository.findById(session.getCourseId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Khóa học không tồn tại: " + session.getCourseId()));

        if (!currentUser.getUserId().equalsIgnoreCase(course.getLecturerId()) && !"ROLE_ADMIN".equalsIgnoreCase(currentUser.getRoleId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền đóng buổi điểm danh này.");
        }

        session.setStatus("CLOSED");
        boolean updated = attendanceSessionRepository.update(sessionId, session);
        if (!updated) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể cập nhật trạng thái buổi điểm danh trên Google Sheets.");
        }

        syncLogService.logSync("CLOSE_SESSION", "SUCCESS", "Closed session " + sessionId);

        String qrBase64 = QRCodeGenerator.generateQRCodeBase64(session.getQrCodeSecret(), 250, 250);
        return mapToResponse(session, qrBase64);
    }

    private User getCurrentUser(String username) throws IOException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại trong hệ thống."));
    }

    private SessionResponse mapToResponse(AttendanceSession session, String qrBase64) {
        return SessionResponse.builder()
                .sessionId(session.getSessionId())
                .courseId(session.getCourseId())
                .slotNumber(session.getSlotNumber())
                .qrCodeSecret(session.getQrCodeSecret())
                .qrCodeBase64(qrBase64)
                .roomNetworkIp(session.getRoomNetworkIp())
                .expiredAt(session.getExpiredAt())
                .status(session.getStatus())
                .createdAt(session.getCreatedAt())
                .build();
    }
}
