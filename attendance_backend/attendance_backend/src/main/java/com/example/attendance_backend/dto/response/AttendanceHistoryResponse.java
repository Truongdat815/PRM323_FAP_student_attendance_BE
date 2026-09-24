package com.example.attendance_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO trả về lịch sử điểm danh cá nhân của sinh viên.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceHistoryResponse {

    private String recordId;
    private String courseId;
    private String subjectCode;
    private String className;
    private Integer slotNumber;
    private LocalDateTime checkedAt;
    private String status;
}
