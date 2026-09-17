package com.example.attendance_backend.service.impl;

import com.example.attendance_backend.dto.response.SyncLogResponse;
import com.example.attendance_backend.model.GoogleSheetSyncLog;
import com.example.attendance_backend.repository.SyncLogRepository;
import com.example.attendance_backend.service.SyncLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SyncLogServiceImpl implements SyncLogService {

    private final SyncLogRepository syncLogRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public SyncLogServiceImpl(SyncLogRepository syncLogRepository) {
        this.syncLogRepository = syncLogRepository;
    }

    @Override
    public void logSync(String action, String status, String details) throws IOException {
        String logId = "LOG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String now = LocalDateTime.now().format(FORMATTER);

        GoogleSheetSyncLog log = GoogleSheetSyncLog.builder()
                .logId(logId)
                .action(action != null ? action : "N/A")
                .status(status != null ? status : "UNKNOWN")
                .details(details != null ? details : "")
                .syncedAt(now)
                .build();

        syncLogRepository.save(log);
    }

    @Override
    public List<SyncLogResponse> getAllLogs() throws IOException {
        List<GoogleSheetSyncLog> logs = syncLogRepository.findAll();
        return logs.stream()
                .map(log -> SyncLogResponse.builder()
                        .logId(log.getLogId())
                        .action(log.getAction())
                        .status(log.getStatus())
                        .details(log.getDetails())
                        .syncedAt(log.getSyncedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
