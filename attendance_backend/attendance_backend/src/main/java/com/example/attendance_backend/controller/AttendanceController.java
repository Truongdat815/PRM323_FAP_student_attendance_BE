package com.example.attendance_backend.controller;

import com.example.attendance_backend.dto.request.CheckInRequest;
import com.example.attendance_backend.dto.response.ApiResponse;
import com.example.attendance_backend.dto.response.AttendanceRecordResponse;
import com.example.attendance_backend.service.AttendanceRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@Tag(name = "Attendance Controller", description = "API thực hiện điểm danh qua QR, tra cứu và xuất báo cáo CSV")
@SecurityRequirement(name = "Bearer Authentication")
public class AttendanceController {

    private final AttendanceRecordService attendanceRecordService;

    @Autowired
    public AttendanceController(AttendanceRecordService attendanceRecordService) {
        this.attendanceRecordService = attendanceRecordService;
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Sinh viên điểm danh qua mã QR", description = "Sinh viên thực hiện điểm danh qua mã QR (kiểm tra 5 điều kiện nghiệp vụ)")
    public ResponseEntity<ApiResponse<AttendanceRecordResponse>> checkIn(
            @Valid @RequestBody CheckInRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) throws IOException {
        String clientIp = httpServletRequest.getRemoteAddr();
        AttendanceRecordResponse response = attendanceRecordService.checkIn(request, authentication.getName(), clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Điểm danh thành công", response));
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
}
