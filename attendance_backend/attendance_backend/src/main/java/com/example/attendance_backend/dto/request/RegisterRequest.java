package com.example.attendance_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận dữ liệu yêu cầu đăng ký tài khoản từ Client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3 đến 50 ký tự")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @NotBlank(message = "Vai trò (roleId: STUDENT hoặc LECTURER) không được để trống")
    private String roleId;

    /**
     * Mã sinh viên (fap_code): Bắt buộc với STUDENT theo regex ^[A-Z]{2}\d{6}$ (Ví dụ: SE192964).
     */
    @Pattern(regexp = "^$|^[A-Z]{2}\\d{6}$", message = "Mã sinh viên (fap_code) phải có định dạng 2 chữ cái in hoa đầu và 6 chữ số (Ví dụ: SE192964)")
    private String fapCode;
}
