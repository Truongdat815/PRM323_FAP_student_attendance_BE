package com.example.attendance_backend.dto.response;

import com.example.attendance_backend.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO trả về thông tin người dùng (không chứa mật khẩu).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String userId;
    private String username;
    private String fapCode;
    private String fullName;
    private String email;
    private String roleId;
    private LocalDateTime createdAt;
    private String memberCode;

    public static UserResponse fromUser(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fapCode(user.getFapCode())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .roleId(user.getRoleId())
                .createdAt(user.getCreatedAt())
                .memberCode(user.getMemberCode())
                .build();
    }
}
