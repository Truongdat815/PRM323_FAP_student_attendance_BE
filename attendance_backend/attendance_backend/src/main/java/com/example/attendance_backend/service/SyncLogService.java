package com.example.attendance_backend.service;

import com.example.attendance_backend.dto.response.SyncLogResponse;

import java.io.IOException;
import java.util.List;

public interface SyncLogService {

    void logSync(String action, String status, String details) throws IOException;

    List<SyncLogResponse> getAllLogs() throws IOException;
}
