package com.example.attendance_backend.service.impl;

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
import com.example.attendance_backend.service.AttendanceRecordService;
import com.example.attendance_backend.service.SyncLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AttendanceRecordServiceImpl implements AttendanceRecordService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final SyncLogService syncLogService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public AttendanceRecordServiceImpl(AttendanceRecordRepository attendanceRecordRepository,
                                       AttendanceSessionRepository attendanceSessionRepository,
                                       EnrollmentRepository enrollmentRepository,
                                       UserRepository userRepository,
                                       SyncLogService syncLogService) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.syncLogService = syncLogService;
    }

    @Override
    public AttendanceRecordResponse checkIn(CheckInRequest request, String currentUsername, String clientIp) throws IOException {
        User student = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Không tìm thấy thông tin sinh viên."));

        AttendanceSession session = attendanceSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Phiên điểm danh không tồn tại: " + request.getSessionId()));

        if (!"ACTIVE".equalsIgnoreCase(session.getStatus())) {
            syncLogService.logSync("CHECK_IN", "FAILED", "Session " + request.getSessionId() + " is not ACTIVE for student " + student.getUserId());
            throw new AppException(HttpStatus.BAD_REQUEST, "Phiên điểm danh hiện không ở trạng thái hoạt động (ACTIVE).");
        }

        if (isExpired(session.getExpiredAt())) {
            session.setStatus("CLOSED");
            attendanceSessionRepository.save(session);
            syncLogService.logSync("CHECK_IN", "FAILED", "Session " + request.getSessionId() + " has expired. Auto-closing.");
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã QR / Phiên điểm danh đã hết hạn.");
        }

        if (request.getQrCodeSecret() == null || !request.getQrCodeSecret().trim().equals(session.getQrCodeSecret())) {
            syncLogService.logSync("CHECK_IN", "FAILED", "Invalid QR code secret for session " + request.getSessionId());
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã QR không hợp lệ hoặc đã hết hạn.");
        }

        boolean isEnrolled = enrollmentRepository.existsByCourseIdAndStudentId(session.getCourseId(), student.getUserId());
        if (!isEnrolled) {
            syncLogService.logSync("CHECK_IN", "FAILED", "Student " + student.getUserId() + " is not enrolled in course " + session.getCourseId());
            throw new AppException(HttpStatus.FORBIDDEN, "Sinh viên chưa đăng ký tham gia khóa học này.");
        }

        boolean alreadyCheckedIn = attendanceRecordRepository.existsBySessionIdAndStudentId(session.getSessionId(), student.getUserId());
        if (alreadyCheckedIn) {
            syncLogService.logSync("CHECK_IN", "FAILED", "Student " + student.getUserId() + " already checked in session " + request.getSessionId());
            throw new AppException(HttpStatus.BAD_REQUEST, "Sinh viên đã thực hiện điểm danh cho buổi học này trước đó.");
        }

        if (session.getRoomNetworkIp() != null && !session.getRoomNetworkIp().trim().isEmpty()) {
            String requiredIp = session.getRoomNetworkIp().trim();
            String effectiveClientIp = (request.getClientIp() != null && !request.getClientIp().trim().isEmpty())
                    ? request.getClientIp().trim()
                    : clientIp;

            if (effectiveClientIp != null && !effectiveClientIp.isEmpty() && !"127.0.0.1".equals(effectiveClientIp) && !"::1".equals(effectiveClientIp)) {
                if (!effectiveClientIp.equals(requiredIp) && !effectiveClientIp.startsWith(requiredIp)) {
                    syncLogService.logSync("CHECK_IN", "FAILED", "IP mismatch. Client: " + effectiveClientIp + ", Required: " + requiredIp);
                    throw new AppException(HttpStatus.FORBIDDEN, "Địa chỉ IP mạng của thiết bị không khớp với mạng phòng học.");
                }
            }
        }

        String recordId = "REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        AttendanceRecord record = AttendanceRecord.builder()
                .recordId(recordId)
                .sessionId(session.getSessionId())
                .studentId(student.getUserId())
                .status("PRESENT")
                .checkedAt(LocalDateTime.now())
                .build();

        attendanceRecordRepository.save(record);
        syncLogService.logSync("CHECK_IN", "SUCCESS", "Student " + student.getFapCode() + " checked in session " + session.getSessionId());

        return AttendanceRecordResponse.builder()
                .recordId(record.getRecordId())
                .sessionId(record.getSessionId())
                .studentId(record.getStudentId())
                .studentFapCode(student.getFapCode())
                .studentName(student.getFullName())
                .status(record.getStatus())
                .checkedAt(record.getCheckedAt())
                .build();
    }

    @Override
    public List<AttendanceRecordResponse> getRecordsBySession(String sessionId) throws IOException {
        List<AttendanceRecord> records = attendanceRecordRepository.findBySessionId(sessionId);
        return records.stream()
                .map(record -> {
                    User student = userRepository.findById(record.getStudentId()).orElse(null);
                    return AttendanceRecordResponse.builder()
                            .recordId(record.getRecordId())
                            .sessionId(record.getSessionId())
                            .studentId(record.getStudentId())
                            .studentFapCode(student != null ? student.getFapCode() : "")
                            .studentName(student != null ? student.getFullName() : "")
                            .status(record.getStatus())
                            .checkedAt(record.getCheckedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public byte[] exportAttendanceToCsv(String sessionId) throws IOException {
        AttendanceSession session = attendanceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy buổi điểm danh với mã: " + sessionId));

        List<AttendanceRecordResponse> records = getRecordsBySession(sessionId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StringBuilder csv = new StringBuilder();
        csv.append("Record ID,Session ID,Student ID,FAP Code,Full Name,Status,Checked At\n");

        for (AttendanceRecordResponse rec : records) {
            String checkedAtStr = rec.getCheckedAt() != null ? rec.getCheckedAt().format(FORMATTER) : "";
            csv.append(escapeCsv(rec.getRecordId())).append(",")
                    .append(escapeCsv(rec.getSessionId())).append(",")
                    .append(escapeCsv(rec.getStudentId())).append(",")
                    .append(escapeCsv(rec.getStudentFapCode())).append(",")
                    .append(escapeCsv(rec.getStudentName())).append(",")
                    .append(escapeCsv(rec.getStatus())).append(",")
                    .append(escapeCsv(checkedAtStr)).append("\n");
        }

        syncLogService.logSync("EXPORT_CSV", "SUCCESS", "Exported CSV for session " + sessionId);
        out.write(csv.toString().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private boolean isExpired(LocalDateTime expiredAt) {
        if (expiredAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiredAt);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
