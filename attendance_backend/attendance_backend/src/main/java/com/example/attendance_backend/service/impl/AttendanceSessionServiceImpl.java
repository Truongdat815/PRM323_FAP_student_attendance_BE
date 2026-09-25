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
import com.example.attendance_backend.util.IpUtil;
import com.example.attendance_backend.util.QRCodeGenerator;
import com.example.attendance_backend.util.TotpUtil;
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
        return createSession(request, currentUsername, null);
    }

    @Override
    public SessionResponse createSession(SessionCreateRequest request, String currentUsername, String clientIp) throws IOException {
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
        String sessionCode = String.format("%06d", new java.util.Random().nextInt(1000000));
        String qrSecret = QRCodeGenerator.generateSecret();

        String hostIp = (clientIp != null && !clientIp.trim().isEmpty()) ? clientIp.trim() : "";
        if (IpUtil.isLoopback(hostIp) || hostIp.isEmpty()) {
            String lanIp = IpUtil.getPrimaryLocalIpv4();
            if (lanIp != null && !IpUtil.isLoopback(lanIp)) {
                hostIp = lanIp;
            }
        }

        String roomIp = (request.getRoomNetworkIp() != null && !request.getRoomNetworkIp().trim().isEmpty())
                ? request.getRoomNetworkIp().trim()
                : hostIp;
        String roomBssid = (request.getRoomBssid() != null && IpUtil.isValidBssid(request.getRoomBssid()))
                ? request.getRoomBssid().trim()
                : "";

        int refreshInterval = (request.getQrRefreshSeconds() != null && request.getQrRefreshSeconds() > 0)
                ? request.getQrRefreshSeconds()
                : TotpUtil.DEFAULT_TIME_STEP_SECONDS;

        AttendanceSession session = AttendanceSession.builder()
                .sessionId(sessionId)
                .courseId(course.getCourseId())
                .slotNumber(request.getSlotNumber())
                .qrCodeSecret(qrSecret)
                .sessionCode(sessionCode)
                .roomNetworkIp(roomIp)
                .hostPublicIp(hostIp)
                .roomBssid(roomBssid)
                .qrRefreshSeconds(refreshInterval)
                .expiredAt(request.getExpiredAt())
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();

        attendanceSessionRepository.save(session);
        syncLogService.logSync("CREATE_SESSION", "SUCCESS", "Created session " + sessionId + " (code: " + sessionCode + ") for course " + course.getCourseId());

        return mapToResponse(session);
    }

    @Override
    public SessionResponse getSessionById(String sessionId) throws IOException {
        AttendanceSession session = attendanceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy buổi điểm danh với mã: " + sessionId));
        return mapToResponse(session);
    }

    @Override
    public SessionResponse getCurrentQrToken(String sessionId) throws IOException {
        AttendanceSession session = attendanceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy buổi điểm danh với mã: " + sessionId));

        if (!"ACTIVE".equalsIgnoreCase(session.getStatus()) && !"OPEN".equalsIgnoreCase(session.getStatus())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Phiên điểm danh hiện không ở trạng thái hoạt động.");
        }
        if (session.getExpiredAt() != null && LocalDateTime.now().isAfter(session.getExpiredAt())) {
            session.setStatus("CLOSED");
            attendanceSessionRepository.save(session);
            throw new AppException(HttpStatus.BAD_REQUEST, "Phiên điểm danh đã hết hạn.");
        }

        return mapToResponse(session);
    }

    @Override
    public List<SessionResponse> getSessionsByCourse(String courseId) throws IOException {
        List<AttendanceSession> sessions = attendanceSessionRepository.findByCourseId(courseId);
        return sessions.stream()
                .map(this::mapToResponse)
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
        AttendanceSession updated = attendanceSessionRepository.save(session);

        syncLogService.logSync("CLOSE_SESSION", "SUCCESS", "Closed session " + sessionId);

        return mapToResponse(updated);
    }

    private User getCurrentUser(String username) throws IOException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại trong hệ thống."));
    }

    private SessionResponse mapToResponse(AttendanceSession session) {
        String currentQrToken = "";
        int remainingSeconds = 0;
        String qrBase64 = "";

        int interval = (session.getQrRefreshSeconds() != null && session.getQrRefreshSeconds() > 0)
                ? session.getQrRefreshSeconds()
                : TotpUtil.DEFAULT_TIME_STEP_SECONDS;

        if (session.getQrCodeSecret() != null && !session.getQrCodeSecret().trim().isEmpty()) {
            currentQrToken = TotpUtil.generateCurrentToken(session.getQrCodeSecret(), interval);
            remainingSeconds = TotpUtil.getRemainingSeconds(interval);
            // Render QR Code Base64 của mã token động hiện tại
            qrBase64 = QRCodeGenerator.generateQRCodeBase64(currentQrToken, 250, 250);
        }

        return SessionResponse.builder()
                .sessionId(session.getSessionId())
                .courseId(session.getCourseId())
                .slotNumber(session.getSlotNumber())
                .qrCodeSecret(session.getQrCodeSecret())
                .qrCodeBase64(qrBase64)
                .roomNetworkIp(session.getRoomNetworkIp())
                .hostPublicIp(session.getHostPublicIp())
                .roomBssid(session.getRoomBssid())
                .currentQrToken(currentQrToken)
                .tokenRemainingSeconds(remainingSeconds)
                .tokenIntervalSeconds(interval)
                .qrRefreshSeconds(interval)
                .expiredAt(session.getExpiredAt())
                .sessionCode(session.getSessionCode())
                .status(session.getStatus())
                .createdAt(session.getCreatedAt())
                .build();
    }
}
