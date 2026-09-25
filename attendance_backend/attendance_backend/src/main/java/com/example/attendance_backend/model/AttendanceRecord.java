package com.example.attendance_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "attendance_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {

    @Id
    @Column(name = "record_id", nullable = false)
    private String recordId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "student_id")
    private String studentId;

    @Column(name = "status")
    private String status;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;
}
