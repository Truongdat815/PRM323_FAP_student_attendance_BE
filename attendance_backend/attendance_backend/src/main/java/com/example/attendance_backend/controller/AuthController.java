package com.example.attendance_backend.controller;

import com.example.attendance_backend.dto.request.LoginRequest;
import com.example.attendance_backend.dto.request.RegisterRequest;
import com.example.attendance_backend.dto.response.ApiResponse;
import com.example.attendance_backend.dto.response.AuthResponse;
import com.example.attendance_backend.dto.response.UserResponse;
import com.example.attendance_backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Controller tiếp nhận các yêu cầu xác thực và đăng ký tài khoản.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication Controller", description = "Các API đăng ký và xác thực tài khoản")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản mới", description = "Cho phép đăng ký tài khoản sinh viên (bắt buộc fap_code regex ^[A-Z]{2}\\d{6}$) hoặc giảng viên")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) throws IOException {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đăng ký tài khoản thành công", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập tài khoản", description = "Xác thực tài khoản và mật khẩu, trả về Bearer JWT Token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) throws IOException {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công", response));
    }
}
