package com.example.attendance_backend.service.impl;

import com.example.attendance_backend.dto.request.RegisterRequest;
import com.example.attendance_backend.dto.response.UserResponse;
import com.example.attendance_backend.exception.AppException;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Lớp triển khai các logic nghiệp vụ đăng ký và xác thực tài khoản.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final Pattern FAP_CODE_PATTERN = Pattern.compile("^[A-Z]{2}\\d{6}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse register(RegisterRequest request) throws IOException {
        String username = request.getUsername().trim();
        String rawRole = request.getRoleId().trim().toUpperCase();

        // 1. Chuẩn hóa Role (STUDENT -> ROLE_STUDENT, LECTURER -> ROLE_LECTURER)
        String roleId = normalizeRole(rawRole);

        // 2. Kiểm tra ràng buộc mã sinh viên fap_code nếu vai trò là STUDENT
        String fapCode = request.getFapCode() != null ? request.getFapCode().trim().toUpperCase() : "";
        if ("ROLE_STUDENT".equalsIgnoreCase(roleId)) {
            if (fapCode.isEmpty()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Mã sinh viên (fap_code) là bắt buộc đối với tài khoản sinh viên.");
            }
            if (!FAP_CODE_PATTERN.matcher(fapCode).matches()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Mã sinh viên không đúng định dạng ^[A-Z]{2}\\d{6}$ (Ví dụ: SE192964).");
            }
        }

        // 3. Kiểm tra trùng lặp username trên Google Sheets USERS
        if (userRepository.existsByUsername(username)) {
            throw new AppException(HttpStatus.CONFLICT, "Tên tài khoản (username) '" + username + "' đã được sử dụng.");
        }

        // 4. Kiểm tra trùng lặp fap_code nếu có giá trị
        if (!fapCode.isEmpty() && userRepository.existsByFapCode(fapCode)) {
            throw new AppException(HttpStatus.CONFLICT, "Mã sinh viên (fap_code) '" + fapCode + "' đã tồn tại trong hệ thống.");
        }

        // 5. Mã hóa mật khẩu bằng BCrypt
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // 6. Khởi tạo đối tượng User mới
        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String now = Instant.now().toString();

        User newUser = User.builder()
                .userId(userId)
                .username(username)
                .passwordHash(passwordHash)
                .fapCode(fapCode)
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .roleId(roleId)
                .createdAt(now)
                .build();

        // 7. Lưu xuống sheet USERS thông qua repository
        userRepository.save(newUser);
        log.info("✅ Đăng ký tài khoản thành công: username={}, role={}, userId={}", username, roleId, userId);

        return UserResponse.fromUser(newUser);
    }

    private String normalizeRole(String rawRole) {
        if (rawRole.startsWith("ROLE_")) {
            return rawRole;
        }
        if ("STUDENT".equalsIgnoreCase(rawRole) || "LECTURER".equalsIgnoreCase(rawRole) || "ADMIN".equalsIgnoreCase(rawRole)) {
            return "ROLE_" + rawRole;
        }
        throw new AppException(HttpStatus.BAD_REQUEST, "Vai trò '" + rawRole + "' không hợp lệ. Chỉ chấp nhận: STUDENT, LECTURER, ADMIN.");
    }
}
