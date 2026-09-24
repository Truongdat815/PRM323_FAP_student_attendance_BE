package com.example.attendance_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

    private String sessionId;
    private String courseId;
    private Integer slotNumber;
    private String qrCodeSecret;
    private String qrCodeBase64;
    private String roomNetworkIp;
    private LocalDateTime expiredAt;
    private String sessionCode;
    private String status;
    private LocalDateTime createdAt;
}
