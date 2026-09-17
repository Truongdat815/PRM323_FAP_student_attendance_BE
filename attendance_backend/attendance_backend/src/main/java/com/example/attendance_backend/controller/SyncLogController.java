package com.example.attendance_backend.controller;

import com.example.attendance_backend.dto.response.ApiResponse;
import com.example.attendance_backend.dto.response.SyncLogResponse;
import com.example.attendance_backend.service.SyncLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/sync-logs")
@Tag(name = "Sync Log Controller", description = "API tra cứu nhật ký đồng bộ và thao tác hệ thống")
@SecurityRequirement(name = "Bearer Authentication")
public class SyncLogController {

    private final SyncLogService syncLogService;

    @Autowired
    public SyncLogController(SyncLogService syncLogService) {
        this.syncLogService = syncLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    @Operation(summary = "Xem nhật ký đồng bộ", description = "Lấy tất cả các bản ghi nhật ký đồng bộ từ sheet GOOGLE_SHEET_SYNC_LOGS")
    public ResponseEntity<ApiResponse<List<SyncLogResponse>>> getAllLogs() throws IOException {
        List<SyncLogResponse> logs = syncLogService.getAllLogs();
        return ResponseEntity.ok(ApiResponse.ok("Lấy nhật ký đồng bộ thành công", logs));
    }
}
