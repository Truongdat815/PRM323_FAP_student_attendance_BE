package com.example.attendance_backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận yêu cầu điểm danh từ sinh viên (qua mã QR hoặc mã PIN sessionCode 6 số).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRequest {

    private String sessionId;

    private String qrCodeSecret;

    private String sessionCode;

    private String clientIp;
}
