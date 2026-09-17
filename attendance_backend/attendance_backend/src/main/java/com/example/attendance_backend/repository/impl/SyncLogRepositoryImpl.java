package com.example.attendance_backend.repository.impl;

import com.example.attendance_backend.model.GoogleSheetSyncLog;
import com.example.attendance_backend.repository.SyncLogRepository;
import com.example.attendance_backend.service.GoogleSheetsService;
import com.example.attendance_backend.util.SheetConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SyncLogRepositoryImpl implements SyncLogRepository {

    private final GoogleSheetsService googleSheetsService;

    @Autowired
    public SyncLogRepositoryImpl(GoogleSheetsService googleSheetsService) {
        this.googleSheetsService = googleSheetsService;
    }

    @Override
    public GoogleSheetSyncLog save(GoogleSheetSyncLog log) throws IOException {
        googleSheetsService.appendRow(SheetConstants.SHEET_GOOGLE_SHEET_SYNC_LOGS, log.toRow());
        return log;
    }

    @Override
    public List<GoogleSheetSyncLog> findAll() throws IOException {
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_GOOGLE_SHEET_SYNC_LOGS);
        List<GoogleSheetSyncLog> logs = new ArrayList<>();
        for (List<Object> row : rows) {
            GoogleSheetSyncLog log = GoogleSheetSyncLog.fromRow(row);
            if (log != null && log.getLogId() != null && !log.getLogId().isEmpty()) {
                logs.add(log);
            }
        }
        return logs;
    }
}
