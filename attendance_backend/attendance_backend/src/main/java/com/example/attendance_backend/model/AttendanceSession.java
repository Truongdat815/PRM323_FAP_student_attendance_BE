package com.example.attendance_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSession {

    @Id
    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "course_id")
    private String courseId;

    @Column(name = "slot_number")
    private Integer slotNumber;

    @Column(name = "qr_code_secret")
    private String qrCodeSecret;

    @Column(name = "room_network_ip")
    private String roomNetworkIp;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
