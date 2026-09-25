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
@Table(name = "google_sheet_sync_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleSheetSyncLog {

    @Id
    @Column(name = "log_id", nullable = false)
    private String logId;

    @Column(name = "action")
    private String action;

    @Column(name = "status")
    private String status;

    @Column(name = "details")
    private String details;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;
}
