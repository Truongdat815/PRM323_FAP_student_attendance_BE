package com.example.attendance_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận dữ liệu cập nhật thông tin cá nhân.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    /**
     * Tên đăng nhập mới (tối thiểu 3 ký tự, tối đa 50 ký tự, không bắt buộc).
     */
    @Pattern(regexp = "^$|^.{3,50}$", message = "Tên đăng nhập (username) phải từ 3 đến 50 ký tự")
    private String username;

    /**
     * Họ và tên mới (không bắt buộc).
     */
    private String fullName;

    /**
     * Email mới (không bắt buộc).
     */
    @Email(message = "Email không đúng định dạng")
    private String email;
}
