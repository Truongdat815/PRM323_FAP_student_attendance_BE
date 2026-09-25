package com.example.attendance_backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận yêu cầu điểm danh từ sinh viên (qua mã QR động TOTP, mã PIN sessionCode 6 số, hoặc mã QR cũ).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRequest {

    private String sessionId;

    /**
     * Mã bí mật QR Code (hỗ trợ backward compatibility).
     */
    private String qrCodeSecret;

    /**
     * Mã Dynamic QR Token (sinh theo thuật toán TOTP đổi mỗi 10 giây).
     */
    private String qrToken;

    /**
     * Mã PIN 6 chữ số của buổi học.
     */
    private String sessionCode;

    /**
     * BSSID (địa chỉ MAC) của Access Point Wi-Fi phòng học lấy từ Mobile App.
     */
    private String bssid;

    /**
     * Mã định danh phần cứng của thiết bị (Device ID) để chống 1 máy điểm danh cho nhiều tài khoản.
     */
    private String deviceId;

    /**
     * @deprecated Địa chỉ IP client (Server sẽ tự động bóc tách từ HttpServletRequest để bảo mật, tránh spoofing).
     */
    @Deprecated
    private String clientIp;
}
