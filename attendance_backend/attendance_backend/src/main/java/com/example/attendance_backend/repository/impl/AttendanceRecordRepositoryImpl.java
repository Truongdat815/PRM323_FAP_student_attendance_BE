package com.example.attendance_backend.repository.impl;

import com.example.attendance_backend.model.AttendanceRecord;
import com.example.attendance_backend.repository.AttendanceRecordRepository;
import com.example.attendance_backend.service.GoogleSheetsService;
import com.example.attendance_backend.util.SheetConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AttendanceRecordRepositoryImpl implements AttendanceRecordRepository {

    private final GoogleSheetsService googleSheetsService;

    @Autowired
    public AttendanceRecordRepositoryImpl(GoogleSheetsService googleSheetsService) {
        this.googleSheetsService = googleSheetsService;
    }

    @Override
    public AttendanceRecord save(AttendanceRecord record) throws IOException {
        googleSheetsService.appendRow(SheetConstants.SHEET_ATTENDANCE_RECORDS, record.toRow());
        return record;
    }

    @Override
    public Optional<AttendanceRecord> findById(String recordId) throws IOException {
        if (recordId == null || recordId.trim().isEmpty()) {
            return Optional.empty();
        }
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_ATTENDANCE_RECORDS);
        for (List<Object> row : rows) {
            if (!row.isEmpty() && recordId.equalsIgnoreCase(row.get(0).toString().trim())) {
                return Optional.ofNullable(AttendanceRecord.fromRow(row));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<AttendanceRecord> findBySessionId(String sessionId) throws IOException {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_ATTENDANCE_RECORDS);
        List<AttendanceRecord> records = new ArrayList<>();
        for (List<Object> row : rows) {
            if (row.size() > 1 && sessionId.equalsIgnoreCase(row.get(1).toString().trim())) {
                AttendanceRecord record = AttendanceRecord.fromRow(row);
                if (record != null) {
                    records.add(record);
                }
            }
        }
        return records;
    }

    @Override
    public List<AttendanceRecord> findByStudentId(String studentId) throws IOException {
        if (studentId == null || studentId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_ATTENDANCE_RECORDS);
        List<AttendanceRecord> records = new ArrayList<>();
        for (List<Object> row : rows) {
            if (row.size() > 2 && studentId.equalsIgnoreCase(row.get(2).toString().trim())) {
                AttendanceRecord record = AttendanceRecord.fromRow(row);
                if (record != null) {
                    records.add(record);
                }
            }
        }
        return records;
    }

    @Override
    public boolean existsBySessionIdAndStudentId(String sessionId, String studentId) throws IOException {
        if (sessionId == null || studentId == null) {
            return false;
        }
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_ATTENDANCE_RECORDS);
        for (List<Object> row : rows) {
            if (row.size() > 2) {
                String sessId = row.get(1).toString().trim();
                String studId = row.get(2).toString().trim();
                if (sessionId.equalsIgnoreCase(sessId) && studentId.equalsIgnoreCase(studId)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<AttendanceRecord> findAll() throws IOException {
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_ATTENDANCE_RECORDS);
        List<AttendanceRecord> records = new ArrayList<>();
        for (List<Object> row : rows) {
            AttendanceRecord record = AttendanceRecord.fromRow(row);
            if (record != null && record.getRecordId() != null && !record.getRecordId().isEmpty()) {
                records.add(record);
            }
        }
        return records;
    }
}
