package com.example.attendance_backend.service;

import com.example.attendance_backend.dto.request.LoginRequest;
import com.example.attendance_backend.dto.request.RegisterRequest;
import com.example.attendance_backend.dto.response.AuthResponse;
import com.example.attendance_backend.dto.response.UserResponse;

import java.io.IOException;

/**
 * Interface cho các dịch vụ xác thực và đăng ký người dùng.
 */
public interface AuthService {

    /**
     * Đăng ký tài khoản người dùng mới (STUDENT hoặc LECTURER).
     *
     * @param request Thông tin đăng ký
     * @return Thông tin người dùng đã tạo thành công (không có mật khẩu)
     */
    UserResponse register(RegisterRequest request) throws IOException;

    /**
     * Đăng nhập người dùng, so khớp mật khẩu và sinh mã JWT Token.
     *
     * @param request Thông tin đăng nhập (username, password)
     * @return AuthResponse chứa JWT Token và thông tin người dùng
     */
    AuthResponse login(LoginRequest request) throws IOException;
}
