package com.example.attendance_backend.service.impl;

import com.example.attendance_backend.dto.request.CheckInRequest;
import com.example.attendance_backend.dto.response.AttendanceHistoryResponse;
import com.example.attendance_backend.dto.response.AttendanceRecordResponse;
import com.example.attendance_backend.exception.AppException;
import com.example.attendance_backend.model.AttendanceRecord;
import com.example.attendance_backend.model.AttendanceSession;
import com.example.attendance_backend.model.Course;
import com.example.attendance_backend.model.Enrollment;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.AttendanceRecordRepository;
import com.example.attendance_backend.repository.AttendanceSessionRepository;
import com.example.attendance_backend.repository.CourseRepository;
import com.example.attendance_backend.repository.EnrollmentRepository;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.service.AttendanceRecordService;
import com.example.attendance_backend.service.SyncLogService;
import com.example.attendance_backend.util.IpUtil;
import com.example.attendance_backend.util.TotpUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceRecordServiceImpl implements AttendanceRecordService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final SyncLogService syncLogService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public AttendanceRecordServiceImpl(AttendanceRecordRepository attendanceRecordRepository,
                                       AttendanceSessionRepository attendanceSessionRepository,
                                       CourseRepository courseRepository,
                                       EnrollmentRepository enrollmentRepository,
                                       UserRepository userRepository,
                                       SyncLogService syncLogService) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.syncLogService = syncLogService;
    }

    public AttendanceRecordServiceImpl(AttendanceRecordRepository attendanceRecordRepository,
                                       AttendanceSessionRepository attendanceSessionRepository,
                                       EnrollmentRepository enrollmentRepository,
                                       UserRepository userRepository,
                                       SyncLogService syncLogService) {
        this(attendanceRecordRepository, attendanceSessionRepository, null, enrollmentRepository, userRepository, syncLogService);
    }

    @Override
    public AttendanceRecordResponse checkIn(CheckInRequest request, String currentUsername, String clientIp) throws IOException {
        User student = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Không tìm thấy thông tin sinh viên."));

        // Xác định mã token gửi lên (ưu tiên qrToken mới, fallback qrCodeSecret cũ)
        String inputToken = (request.getQrToken() != null && !request.getQrToken().trim().isEmpty())
                ? request.getQrToken().trim()
                : (request.getQrCodeSecret() != null && !request.getQrCodeSecret().trim().isEmpty() ? request.getQrCodeSecret().trim() : null);

        boolean hasQr = inputToken != null && !inputToken.isEmpty();
        boolean hasPin = request.getSessionCode() != null && !request.getSessionCode().trim().isEmpty();

        if (!hasQr && !hasPin) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Vui lòng cung cấp mã QR (qrToken) hoặc mã PIN buổi học (sessionCode).");
        }

        AttendanceSession session;
        if (request.getSessionId() != null && !request.getSessionId().trim().isEmpty()) {
            session = attendanceSessionRepository.findById(request.getSessionId().trim())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Phiên điểm danh không tồn tại: " + request.getSessionId()));
        } else if (hasPin) {
            String code = request.getSessionCode().trim();
            List<AttendanceSession> activeSessions = attendanceSessionRepository.findBySessionCodeAndStatus(code, "ACTIVE");
            if (activeSessions.isEmpty()) {
                activeSessions = attendanceSessionRepository.findBySessionCodeAndStatus(code, "OPEN");
            }
            if (activeSessions.isEmpty()) {
                throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên điểm danh đang mở với mã: " + code);
            }
            if (activeSessions.size() > 1) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Có nhiều hơn 1 phiên điểm danh đang mở với mã code này. Vui lòng cung cấp thêm sessionId.");
            }
            session = activeSessions.get(0);
        } else {
            throw new AppException(HttpStatus.BAD_REQUEST, "Thiếu thông tin phiên điểm danh (sessionId).");
        }

        if (!"ACTIVE".equalsIgnoreCase(session.getStatus()) && !"OPEN".equalsIgnoreCase(session.getStatus())) {
            syncLogService.logSync("CHECK_IN", "FAILED", "Session " + session.getSessionId() + " is not ACTIVE for student " + student.getUserId());
            throw new AppException(HttpStatus.BAD_REQUEST, "Phiên điểm danh hiện không ở trạng thái hoạt động.");
        }

        if (isExpired(session.getExpiredAt())) {
            session.setStatus("CLOSED");
            attendanceSessionRepository.save(session);
            syncLogService.logSync("CHECK_IN", "FAILED", "Session " + session.getSessionId() + " has expired. Auto-closing.");
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã QR / Phiên điểm danh đã hết hạn.");
        }

        // 1. Kiểm tra tính hợp lệ của PIN code hoặc Dynamic QR code
        if (hasPin) {
            if (session.getSessionCode() == null || !session.getSessionCode().trim().equals(request.getSessionCode().trim())) {
                syncLogService.logSync("CHECK_IN", "FAILED", "Invalid sessionCode for session " + session.getSessionId());
                throw new AppException(HttpStatus.BAD_REQUEST, "Mã PIN điểm danh (sessionCode) không chính xác.");
            }
        } else {
            int interval = (session.getQrRefreshSeconds() != null && session.getQrRefreshSeconds() > 0)
                    ? session.getQrRefreshSeconds() : TotpUtil.DEFAULT_TIME_STEP_SECONDS;

            // Xác thực Dynamic TOTP token (window = 1 chu kỳ ~ interval giây để bù trễ mạng)
            boolean isValidTotp = TotpUtil.verifyToken(session.getQrCodeSecret(), inputToken, interval, TotpUtil.DEFAULT_TOLERANCE_WINDOW);
            boolean isLegacySecret = session.getQrCodeSecret() != null && session.getQrCodeSecret().equalsIgnoreCase(inputToken);

            if (!isValidTotp && !isLegacySecret) {
                syncLogService.logSync("CHECK_IN", "FAILED", "Invalid or expired QR token for session " + session.getSessionId());
                throw new AppException(HttpStatus.BAD_REQUEST, "Mã QR không hợp lệ hoặc đã hết hạn (mã QR tự làm mới mỗi " + interval + " giây). Vui lòng quét lại.");
            }
        }

        // 2. Kiểm tra ghi danh môn học (Enrollment)
        boolean isEnrolled = enrollmentRepository.existsByCourseIdAndStudentId(session.getCourseId(), student.getUserId());
        if (!isEnrolled) {
            syncLogService.logSync("CHECK_IN", "FAILED", "Student " + student.getUserId() + " is not enrolled in course " + session.getCourseId());
            throw new AppException(HttpStatus.FORBIDDEN, "Sinh viên chưa đăng ký tham gia khóa học này.");
        }

        // 3. Kiểm tra trùng lặp sinh viên (Anti-Spam)
        boolean alreadyCheckedIn = attendanceRecordRepository.existsBySessionIdAndStudentId(session.getSessionId(), student.getUserId());
        if (alreadyCheckedIn) {
            syncLogService.logSync("CHECK_IN", "FAILED", "Student " + student.getUserId() + " already checked in session " + session.getSessionId());
            throw new AppException(HttpStatus.BAD_REQUEST, "Sinh viên đã thực hiện điểm danh cho buổi học này trước đó.");
        }

        // 4. Chống điểm danh hộ trên cùng một thiết bị (Device Binding)
        String deviceId = request.getDeviceId() != null ? request.getDeviceId().trim() : null;
        if (deviceId != null && !deviceId.isEmpty()) {
            boolean deviceAlreadyUsed = attendanceRecordRepository.existsBySessionIdAndDeviceId(session.getSessionId(), deviceId);
            if (deviceAlreadyUsed) {
                syncLogService.logSync("CHECK_IN", "FAILED", "Device " + deviceId + " already used in session " + session.getSessionId());
                throw new AppException(HttpStatus.BAD_REQUEST, "Thiết bị này đã được sử dụng để điểm danh cho một sinh viên khác trong buổi học này.");
            }
        }

        // 5. Kiểm tra kết nối mạng Wi-Fi (Ưu tiên BSSID -> Fallback Public IP Subnet)
        boolean wifiVerified = false;

        // Ưu tiên 1: Kiểm tra BSSID từ Router Wi-Fi phòng học nếu buổi học có cấu hình BSSID hợp lệ
        if (IpUtil.isValidBssid(session.getRoomBssid())) {
            String requiredBssid = session.getRoomBssid().trim();
            String clientBssid = request.getBssid() != null ? request.getBssid().trim() : "";

            if (requiredBssid.equalsIgnoreCase(clientBssid)) {
                wifiVerified = true;
            } else if (IpUtil.isValidBssid(clientBssid)) {
                // Sinh viên gửi lên một BSSID hợp lệ khác -> Chắc chắn đang kết nối mạng Wi-Fi khác
                syncLogService.logSync("CHECK_IN", "FAILED", "BSSID mismatch. Client: " + clientBssid + ", Required: " + requiredBssid);
                throw new AppException(HttpStatus.BAD_REQUEST, "Thiết bị không kết nối đúng mạng Wi-Fi phòng học (BSSID không khớp).");
            }
            // Nếu clientBssid rỗng hoặc dummy (ví dụ 02:00:00:00:00:00 do Android tắt GPS/chưa cấp quyền vị trí),
            // sẽ tự động rơi xuống bước kiểm tra IP bên dưới để không chặn nhầm sinh viên đang ngồi trong lớp.
        }

        // Ưu tiên 2: Kiểm tra địa chỉ IP nếu chưa xác thực qua BSSID
        String targetHostIp = (session.getHostPublicIp() != null && !session.getHostPublicIp().trim().isEmpty())
                ? session.getHostPublicIp().trim()
                : (session.getRoomNetworkIp() != null ? session.getRoomNetworkIp().trim() : "");

        if (!wifiVerified && !targetHostIp.isEmpty()) {
            // Server luôn sử dụng clientIp thực từ socket/proxy header, không dùng request.getClientIp() để chống spoofing
            if (!IpUtil.isNetworkMatching(clientIp, targetHostIp)) {
                syncLogService.logSync("CHECK_IN", "FAILED", "IP mismatch. Client: " + clientIp + ", Required: " + targetHostIp);
                throw new AppException(HttpStatus.BAD_REQUEST, "Thiết bị không kết nối cùng mạng Wi-Fi với giảng viên (Địa chỉ IP không khớp).");
            }
        }

        String recordId = "REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        AttendanceRecord record = AttendanceRecord.builder()
                .recordId(recordId)
                .sessionId(session.getSessionId())
                .studentId(student.getUserId())
                .deviceId(deviceId)
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
                .deviceId(record.getDeviceId())
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

    @Override
    public List<AttendanceHistoryResponse> getMyAttendanceHistory(String currentUsername, String courseId, String subjectCode, LocalDate startDate, LocalDate endDate) throws IOException {
        User student = userRepository.findByUsername(currentUsername)
                .or(() -> userRepository.findById(currentUsername))
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Không tìm thấy thông tin sinh viên."));

        List<AttendanceRecord> records = attendanceRecordRepository.findByStudentId(student.getUserId());

        // Sắp xếp thời gian mới nhất lên đầu
        records.sort((r1, r2) -> {
            if (r1.getCheckedAt() == null || r2.getCheckedAt() == null) return 0;
            return r2.getCheckedAt().compareTo(r1.getCheckedAt());
        });

        // Batch load các session và course tương ứng
        List<String> sessionIds = records.stream().map(AttendanceRecord::getSessionId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<String, AttendanceSession> sessionMap = attendanceSessionRepository.findAllById(sessionIds).stream()
                .collect(Collectors.toMap(AttendanceSession::getSessionId, s -> s));

        List<String> courseIds = sessionMap.values().stream().map(AttendanceSession::getCourseId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<String, Course> courseMap = courseRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(Course::getCourseId, c -> c));

        return records.stream()
                .map(record -> {
                    AttendanceSession session = sessionMap.get(record.getSessionId());
                    Course course = (session != null) ? courseMap.get(session.getCourseId()) : null;

                    // Lọc theo courseId
                    if (courseId != null && !courseId.trim().isEmpty()) {
                        if (course == null || !course.getCourseId().equalsIgnoreCase(courseId.trim())) {
                            return null;
                        }
                    }

                    // Lọc theo subjectCode
                    if (subjectCode != null && !subjectCode.trim().isEmpty()) {
                        if (course == null || course.getSubjectCode() == null || !course.getSubjectCode().equalsIgnoreCase(subjectCode.trim())) {
                            return null;
                        }
                    }

                    // Lọc theo startDate
                    if (startDate != null && record.getCheckedAt() != null) {
                        if (record.getCheckedAt().toLocalDate().isBefore(startDate)) {
                            return null;
                        }
                    }

                    // Lọc theo endDate
                    if (endDate != null && record.getCheckedAt() != null) {
                        if (record.getCheckedAt().toLocalDate().isAfter(endDate)) {
                            return null;
                        }
                    }

                    return AttendanceHistoryResponse.builder()
                            .recordId(record.getRecordId())
                            .courseId(course != null ? course.getCourseId() : (session != null ? session.getCourseId() : null))
                            .subjectCode(course != null ? course.getSubjectCode() : null)
                            .className(course != null ? course.getClassName() : null)
                            .slotNumber(session != null ? session.getSlotNumber() : null)
                            .checkedAt(record.getCheckedAt())
                            .status(record.getStatus())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public byte[] exportAttendanceToExcel(String sessionId) throws IOException {
        AttendanceSession session = attendanceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy buổi điểm danh với mã: " + sessionId));

        Course course = courseRepository.findById(session.getCourseId()).orElse(null);

        // Lấy danh sách 100% sinh viên đã đăng ký khóa học
        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(session.getCourseId());

        // Lấy danh sách các bản ghi điểm danh thực tế
        List<AttendanceRecord> records = attendanceRecordRepository.findBySessionId(sessionId);
        Map<String, AttendanceRecord> recordMap = records.stream()
                .collect(Collectors.toMap(AttendanceRecord::getStudentId, r -> r, (r1, r2) -> r1));

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String sheetTitle = "Attendance_" + (course != null && course.getClassName() != null ? course.getClassName() : "Session");
            Sheet sheet = workbook.createSheet(sheetTitle);

            // Tạo Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Viết dòng Header
            Row headerRow = sheet.createRow(0);
            String[] headers = {"STT", "Mã sinh viên", "Họ và tên", "Trạng thái", "Thời gian điểm danh"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Enrollment enrollment : enrollments) {
                User student = userRepository.findById(enrollment.getStudentId()).orElse(null);
                AttendanceRecord record = recordMap.get(enrollment.getStudentId());

                Row row = sheet.createRow(rowIdx);
                row.createCell(0).setCellValue(rowIdx);

                String fapCode = (student != null && student.getFapCode() != null)
                        ? student.getFapCode()
                        : (student != null ? student.getUsername() : enrollment.getStudentId());
                row.createCell(1).setCellValue(fapCode);

                String fullName = (student != null && student.getFullName() != null)
                        ? student.getFullName()
                        : "";
                row.createCell(2).setCellValue(fullName);

                Cell statusCell = row.createCell(3);
                Cell timeCell = row.createCell(4);

                if (record != null) {
                    statusCell.setCellValue("PRESENT");
                    timeCell.setCellValue(record.getCheckedAt() != null ? record.getCheckedAt().format(FORMATTER) : "");
                } else {
                    statusCell.setCellValue("ABSENT");
                    timeCell.setCellValue("");
                }

                rowIdx++;
            }

            // Tự động căn chỉnh độ rộng cột
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            syncLogService.logSync("EXPORT_EXCEL", "SUCCESS", "Exported Excel attendance for session " + sessionId);
            return out.toByteArray();
        }
    }

    private boolean isNetworkMatching(String clientIp, String requiredIp) {
        if (clientIp == null || clientIp.trim().isEmpty() ||
                "127.0.0.1".equals(clientIp) || "0:0:0:0:0:0:0:1".equals(clientIp) || "::1".equals(clientIp)) {
            return true; // Chấp nhận localhost cho kiểm thử và môi trường phát triển
        }

        String cIp = clientIp.trim();
        String rIp = requiredIp.trim();

        if (cIp.equalsIgnoreCase(rIp)) {
            return true;
        }

        if (rIp.contains("/")) {
            String base = rIp.substring(0, rIp.indexOf('/'));
            int lastDot = base.lastIndexOf('.');
            if (lastDot > 0) {
                return cIp.startsWith(base.substring(0, lastDot + 1));
            }
        }

        if (rIp.endsWith(".0")) {
            String prefix = rIp.substring(0, rIp.length() - 1);
            return cIp.startsWith(prefix);
        }

        return cIp.startsWith(rIp);
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
