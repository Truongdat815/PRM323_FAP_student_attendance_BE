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
public class AttendanceRecordResponse {

    private String recordId;
    private String sessionId;
    private String studentId;
    private String studentFapCode;
    private String studentName;
    private String status;
    private String deviceId;
    private LocalDateTime checkedAt;
}
