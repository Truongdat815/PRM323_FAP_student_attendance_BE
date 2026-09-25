package com.example.attendance_backend.service;

import com.example.attendance_backend.dto.request.ChangePasswordRequest;
import com.example.attendance_backend.dto.request.UpdateProfileRequest;
import com.example.attendance_backend.dto.response.UserResponse;

import java.io.IOException;

/**
 * Interface nghiệp vụ quản lý người dùng (User Profile & Account).
 */
public interface UserService {

    UserResponse updateProfile(UpdateProfileRequest request, String currentUsername) throws IOException;

    void changePassword(ChangePasswordRequest request, String currentUsername) throws IOException;
}
