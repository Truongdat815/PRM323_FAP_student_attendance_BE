package com.example.attendance_backend.controller;

import com.example.attendance_backend.dto.request.CheckInRequest;
import com.example.attendance_backend.dto.response.ApiResponse;
import com.example.attendance_backend.dto.response.AttendanceHistoryResponse;
import com.example.attendance_backend.dto.response.AttendanceRecordResponse;
import com.example.attendance_backend.service.AttendanceRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@Tag(name = "Attendance Controller", description = "API thực hiện điểm danh qua QR / mã PIN, tra cứu và xuất báo cáo CSV / Excel")
@SecurityRequirement(name = "Bearer Authentication")
public class AttendanceController {

    private final AttendanceRecordService attendanceRecordService;

    @Autowired
    public AttendanceController(AttendanceRecordService attendanceRecordService) {
        this.attendanceRecordService = attendanceRecordService;
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Sinh viên điểm danh (qua mã QR hoặc mã PIN sessionCode 6 số)", description = "Sinh viên thực hiện điểm danh qua mã QR hoặc mã PIN sessionCode (kiểm tra điều kiện hoạt động, thời hạn, wifi phòng học và ghi danh)")
    public ResponseEntity<ApiResponse<AttendanceRecordResponse>> checkIn(
            @Valid @RequestBody CheckInRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) throws IOException {
        String clientIp = httpServletRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.trim().isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = httpServletRequest.getRemoteAddr();
        } else if (clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }

        AttendanceRecordResponse response = attendanceRecordService.checkIn(request, authentication.getName(), clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Điểm danh thành công", response));
    }

    @GetMapping("/my-history")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Sinh viên xem lịch sử điểm danh cá nhân", description = "Lấy lịch sử điểm danh của sinh viên hiện tại kèm các bộ lọc tùy chọn theo khóa học, môn học và khoảng thời gian")
    public ResponseEntity<ApiResponse<List<AttendanceHistoryResponse>>> getMyAttendanceHistory(
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) throws IOException {
        List<AttendanceHistoryResponse> history = attendanceRecordService.getMyAttendanceHistory(
                authentication.getName(), courseId, subjectCode, startDate, endDate
        );
        return ResponseEntity.ok(ApiResponse.ok("Lấy lịch sử điểm danh thành công", history));
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    @Operation(summary = "Xem lịch sử điểm danh của buổi học", description = "Lấy danh sách các sinh viên đã thực hiện điểm danh theo sessionId")
    public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getRecordsBySession(@PathVariable String sessionId) throws IOException {
        List<AttendanceRecordResponse> responses = attendanceRecordService.getRecordsBySession(sessionId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy lịch sử điểm danh thành công", responses));
    }

    @GetMapping("/export/{sessionId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    @Operation(summary = "Xuất danh sách điểm danh ra tệp .CSV", description = "Trích xuất toàn bộ dữ liệu điểm danh của một buổi học ra tệp .csv")
    public ResponseEntity<byte[]> exportAttendanceToCsv(@PathVariable String sessionId) throws IOException {
        byte[] csvBytes = attendanceRecordService.exportAttendanceToCsv(sessionId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "attendance_session_" + sessionId + ".csv");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/export-excel/{sessionId}")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    @Operation(summary = "Xuất danh sách điểm danh buổi học ra tệp Excel (.xlsx)", description = "Xuất file Excel hiển thị 100% sinh viên trong danh sách lớp, gồm sinh viên đã điểm danh (PRESENT) và vắng (ABSENT)")
    public ResponseEntity<byte[]> exportAttendanceToExcel(@PathVariable String sessionId) throws IOException {
        byte[] excelBytes = attendanceRecordService.exportAttendanceToExcel(sessionId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "attendance_" + sessionId + ".xlsx");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }
}
