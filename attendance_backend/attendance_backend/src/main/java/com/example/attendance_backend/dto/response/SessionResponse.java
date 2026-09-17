package com.example.attendance_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String expiredAt;
    private String status;
    private String createdAt;
}
