package com.example.attendance_backend.service.impl;

import com.example.attendance_backend.dto.request.ChangePasswordRequest;
import com.example.attendance_backend.dto.request.UpdateProfileRequest;
import com.example.attendance_backend.dto.response.UserResponse;
import com.example.attendance_backend.exception.AppException;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.service.GoogleSheetsService;
import com.example.attendance_backend.service.UserService;
import com.example.attendance_backend.util.SheetConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Triển khai nghiệp vụ quản lý thông tin người dùng và tài khoản.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GoogleSheetsService googleSheetsService;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Autowired(required = false) GoogleSheetsService googleSheetsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.googleSheetsService = googleSheetsService;
    }

    @Override
    public UserResponse updateProfile(UpdateProfileRequest request, String currentUsername) throws IOException {
        User user = getCurrentUser(currentUsername);

        // 1. Cập nhật Username nếu có truyền
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            String newUsername = request.getUsername().trim();
            if (newUsername.length() < 3 || newUsername.length() > 50) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Tên đăng nhập (username) phải từ 3 đến 50 ký tự.");
            }
            if (!newUsername.equalsIgnoreCase(user.getUsername())) {
                userRepository.findByUsername(newUsername).ifPresent(existing -> {
                    if (!existing.getUserId().equals(user.getUserId())) {
                        throw new AppException(HttpStatus.BAD_REQUEST, "Tên đăng nhập đã được sử dụng bởi tài khoản khác.");
                    }
                });
                user.setUsername(newUsername);
            }
        }

        // 2. Cập nhật Họ và tên nếu có truyền
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        // 3. Cập nhật Email nếu có truyền
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().trim();
            if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(newEmail)) {
                userRepository.findByEmail(newEmail).ifPresent(existing -> {
                    if (!existing.getUserId().equals(user.getUserId())) {
                        throw new AppException(HttpStatus.BAD_REQUEST, "Email đã được sử dụng bởi tài khoản khác.");
                    }
                });
                user.setEmail(newEmail);
            }
        }

        userRepository.save(user);

        log.info("✅ Cập nhật thông tin người dùng thành công: userId={}, username={}", user.getUserId(), user.getUsername());

        // Đồng bộ sang Google Sheets tab USERS
        syncUserToGoogleSheets(user);

        return UserResponse.fromUser(user);
    }

    @Override
    public void changePassword(ChangePasswordRequest request, String currentUsername) throws IOException {
        User user = getCurrentUser(currentUsername);

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mật khẩu cũ không chính xác.");
        }

        // Kiểm tra mật khẩu mới không trùng mật khẩu cũ
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được trùng với mật khẩu cũ.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("✅ Đổi mật khẩu thành công cho người dùng: userId={}, username={}", user.getUserId(), user.getUsername());

        // Đồng bộ hash mật khẩu mới sang Google Sheets nếu có kết nối
        syncUserToGoogleSheets(user);
    }

    private User getCurrentUser(String usernameOrId) {
        return userRepository.findByUsername(usernameOrId)
                .or(() -> userRepository.findById(usernameOrId))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng: " + usernameOrId));
    }

    private void syncUserToGoogleSheets(User user) {
        if (googleSheetsService != null && googleSheetsService.isConnected()) {
            try {
                int rowIndex = googleSheetsService.findRowIndex(
                        googleSheetsService.getDefaultSpreadsheetId(),
                        SheetConstants.SHEET_USERS,
                        0,
                        user.getUserId()
                );
                if (rowIndex > 0) {
                    List<Object> rowValues = Arrays.asList(
                            user.getUserId(),
                            user.getUsername(),
                            user.getPasswordHash(),
                            user.getFapCode() != null ? user.getFapCode() : "",
                            user.getFullName() != null ? user.getFullName() : "",
                            user.getEmail() != null ? user.getEmail() : "",
                            user.getRoleId() != null ? user.getRoleId() : "",
                            user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
                    );
                    String range = SheetConstants.SHEET_USERS + "!A" + rowIndex + ":H" + rowIndex;
                    googleSheetsService.updateRow(range, rowValues);
                    log.info("📊 Đã đồng bộ thông tin user lên Google Sheets tại dòng {}", rowIndex);
                }
            } catch (Exception e) {
                log.warn("⚠️ Không thể đồng bộ user lên Google Sheets: {}", e.getMessage());
            }
        }
    }
}
