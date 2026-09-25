package com.example.attendance_backend.controller;

import com.example.attendance_backend.dto.request.SessionCreateRequest;
import com.example.attendance_backend.dto.response.ApiResponse;
import com.example.attendance_backend.dto.response.SessionResponse;
import com.example.attendance_backend.service.AttendanceSessionService;
import com.example.attendance_backend.util.IpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/sessions")
@Tag(name = "Attendance Session Controller", description = "API quản lý buổi học và sinh mã QR điểm danh")
@SecurityRequirement(name = "Bearer Authentication")
public class AttendanceSessionController {

    private final AttendanceSessionService attendanceSessionService;

    @Autowired
    public AttendanceSessionController(AttendanceSessionService attendanceSessionService) {
        this.attendanceSessionService = attendanceSessionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    @Operation(summary = "Tạo buổi điểm danh", description = "Giảng viên tạo buổi điểm danh mới cho khóa học (Slot 1-6, sinh mã QR, tự động ghi nhận Public IP của Wi-Fi giảng viên)")
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @Valid @RequestBody SessionCreateRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) throws IOException {
        String clientIp = IpUtil.extractClientIp(httpServletRequest);
        SessionResponse response = attendanceSessionService.createSession(request, authentication.getName(), clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo buổi điểm danh thành công", response));
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Xem chi tiết buổi điểm danh", description = "Lấy thông tin chi tiết phiên điểm danh kèm mã QR")
    public ResponseEntity<ApiResponse<SessionResponse>> getSessionById(@PathVariable String sessionId) throws IOException {
        SessionResponse response = attendanceSessionService.getSessionById(sessionId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy thông tin buổi điểm danh thành công", response));
    }

    @GetMapping("/{sessionId}/current-token")
    @Operation(summary = "Lấy mã Dynamic QR Token hiện tại", description = "Trả về mã QR Token ngắn hạn theo thuật toán TOTP đổi mỗi 10 giây kèm số giây còn lại")
    public ResponseEntity<ApiResponse<SessionResponse>> getCurrentQrToken(@PathVariable String sessionId) throws IOException {
        SessionResponse response = attendanceSessionService.getCurrentQrToken(sessionId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy mã QR token thành công", response));
    }

    @GetMapping(value = "/{sessionId}/qr-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream SSE mã QR động realtime", description = "Server-Sent Events tự động push token và countdown đếm ngược mỗi 1 giây cho màn hình máy chiếu giảng viên")
    public SseEmitter streamQrToken(@PathVariable String sessionId) {
        SseEmitter emitter = new SseEmitter(15 * 60 * 1000L); // 15 phút timeout
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {
            try {
                SessionResponse response = attendanceSessionService.getCurrentQrToken(sessionId);
                emitter.send(SseEmitter.event()
                        .name("qr-token")
                        .data(response));
            } catch (Exception e) {
                emitter.complete();
                executor.shutdown();
            }
        }, 0, 1, TimeUnit.SECONDS);

        emitter.onCompletion(executor::shutdown);
        emitter.onTimeout(() -> {
            emitter.complete();
            executor.shutdown();
        });
        emitter.onError((ex) -> {
            emitter.complete();
            executor.shutdown();
        });

        return emitter;
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Danh sách buổi điểm danh theo khóa học", description = "Lấy toàn bộ danh sách các buổi điểm danh thuộc khóa học")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessionsByCourse(@PathVariable String courseId) throws IOException {
        List<SessionResponse> responses = attendanceSessionService.getSessionsByCourse(courseId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách buổi điểm danh thành công", responses));
    }

    @PutMapping("/{sessionId}/close")
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    @Operation(summary = "Đóng buổi điểm danh", description = "Chuyển trạng thái buổi điểm danh sang CLOSED")
    public ResponseEntity<ApiResponse<SessionResponse>> closeSession(
            @PathVariable String sessionId,
            Authentication authentication) throws IOException {
        SessionResponse response = attendanceSessionService.closeSession(sessionId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Đóng buổi điểm danh thành công", response));
    }
}
