package com.example.attendance_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInRequest {

    @NotBlank(message = "Session ID không được để trống")
    private String sessionId;

    @NotBlank(message = "Mã QR secret không được để trống")
    private String qrCodeSecret;

    private String clientIp;
}
