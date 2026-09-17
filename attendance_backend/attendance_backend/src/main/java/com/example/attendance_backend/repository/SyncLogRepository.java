package com.example.attendance_backend.repository;

import com.example.attendance_backend.model.GoogleSheetSyncLog;

import java.io.IOException;
import java.util.List;

public interface SyncLogRepository {

    GoogleSheetSyncLog save(GoogleSheetSyncLog log) throws IOException;

    List<GoogleSheetSyncLog> findAll() throws IOException;
}
