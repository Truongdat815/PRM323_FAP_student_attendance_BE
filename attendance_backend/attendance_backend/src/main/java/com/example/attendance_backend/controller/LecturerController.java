package com.example.attendance_backend.controller;

import com.example.attendance_backend.dto.response.ApiResponse;
import com.example.attendance_backend.dto.response.LecturerDashboardStatsResponse;
import com.example.attendance_backend.service.LecturerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Controller cung cấp các API chuyên biệt dành riêng cho Giảng viên (Dashboard, Thống kê).
 */
@RestController
@RequestMapping({"/api/lecturer", "/api/lecturers"})
@Tag(name = "Lecturer Controller", description = "Các API dành riêng cho Giảng viên")
@SecurityRequirement(name = "bearerAuth")
public class LecturerController {

    private final LecturerService lecturerService;

    @Autowired
    public LecturerController(LecturerService lecturerService) {
        this.lecturerService = lecturerService;
    }

    @GetMapping({"/dashboard-stats", "/stats"})
    @PreAuthorize("hasAnyRole('LECTURER', 'ADMIN')")
    @Operation(summary = "Thống kê Dashboard riêng cho Giảng viên",
            description = "Trả về trực tiếp các số liệu thống kê chỉ trong 1 request duy nhất dựa trên JWT của Giảng viên đang đăng nhập. ADMIN có thể truyền thêm lecturerId để xem số liệu của giảng viên bất kỳ.")
    public ResponseEntity<ApiResponse<LecturerDashboardStatsResponse>> getDashboardStats(
            @RequestParam(required = false) String lecturerId,
            Authentication authentication) throws IOException {
        LecturerDashboardStatsResponse response = lecturerService.getDashboardStats(authentication.getName(), lecturerId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy thông tin tổng quan giảng viên thành công", response));
    }
}
