package com.example.attendance_backend.controller;

import com.example.attendance_backend.dto.request.ChangePasswordRequest;
import com.example.attendance_backend.dto.request.UpdateProfileRequest;
import com.example.attendance_backend.dto.response.ApiResponse;
import com.example.attendance_backend.dto.response.UserResponse;
import com.example.attendance_backend.service.ExcelImportService;
import com.example.attendance_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Controller tiếp nhận các yêu cầu quản lý người dùng, hồ sơ cá nhân và import dữ liệu Excel.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "User Controller", description = "Các API quản lý người dùng, hồ sơ cá nhân và import dữ liệu Excel")
public class UserController {

    private final ExcelImportService excelImportService;
    private final UserService userService;

    @Autowired
    public UserController(ExcelImportService excelImportService, UserService userService) {
        this.excelImportService = excelImportService;
        this.userService = userService;
    }

    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import danh sách người dùng từ file Excel", description = "Tải lên file Excel (.xlsx hoặc .xls) theo định dạng (Class, RollNumber, Email, MemberCode, FullName, Password) để lưu vào cơ sở dữ liệu")
    public ResponseEntity<ApiResponse<List<UserResponse>>> importUsersFromExcel(@RequestParam("file") MultipartFile file) throws IOException {
        List<UserResponse> responses = excelImportService.importUsersFromExcel(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Import danh sách người dùng từ file Excel thành công", responses));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('STUDENT', 'LECTURER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Cập nhật thông tin cá nhân", description = "Cho phép người dùng cập nhật họ tên và email của bản thân")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) throws IOException {
        UserResponse response = userService.updateProfile(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin cá nhân thành công", response));
    }

    @PutMapping("/change-password")
    @PreAuthorize("hasAnyRole('STUDENT', 'LECTURER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Đổi mật khẩu tài khoản", description = "Cho phép người dùng đổi mật khẩu khi cung cấp đúng mật khẩu cũ")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) throws IOException {
        userService.changePassword(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công", null));
    }
}

