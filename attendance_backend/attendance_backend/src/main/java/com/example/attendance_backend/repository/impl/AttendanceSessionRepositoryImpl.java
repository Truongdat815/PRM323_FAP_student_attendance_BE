package com.example.attendance_backend.repository.impl;

import com.example.attendance_backend.model.AttendanceSession;
import com.example.attendance_backend.repository.AttendanceSessionRepository;
import com.example.attendance_backend.service.GoogleSheetsService;
import com.example.attendance_backend.util.SheetConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AttendanceSessionRepositoryImpl implements AttendanceSessionRepository {

    private final GoogleSheetsService googleSheetsService;

    @Autowired
    public AttendanceSessionRepositoryImpl(GoogleSheetsService googleSheetsService) {
        this.googleSheetsService = googleSheetsService;
    }

    @Override
    public AttendanceSession save(AttendanceSession session) throws IOException {
        googleSheetsService.appendRow(SheetConstants.SHEET_ATTENDANCE_SESSIONS, session.toRow());
        return session;
    }

    @Override
    public Optional<AttendanceSession> findById(String sessionId) throws IOException {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return Optional.empty();
        }
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_ATTENDANCE_SESSIONS);
        for (List<Object> row : rows) {
            if (!row.isEmpty() && sessionId.equalsIgnoreCase(row.get(0).toString().trim())) {
                return Optional.ofNullable(AttendanceSession.fromRow(row));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<AttendanceSession> findByCourseId(String courseId) throws IOException {
        if (courseId == null || courseId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_ATTENDANCE_SESSIONS);
        List<AttendanceSession> sessions = new ArrayList<>();
        for (List<Object> row : rows) {
            if (row.size() > 1 && courseId.equalsIgnoreCase(row.get(1).toString().trim())) {
                AttendanceSession session = AttendanceSession.fromRow(row);
                if (session != null) {
                    sessions.add(session);
                }
            }
        }
        return sessions;
    }

    @Override
    public List<AttendanceSession> findAll() throws IOException {
        List<List<Object>> rows = googleSheetsService.readSheet(SheetConstants.SHEET_ATTENDANCE_SESSIONS);
        List<AttendanceSession> sessions = new ArrayList<>();
        for (List<Object> row : rows) {
            AttendanceSession session = AttendanceSession.fromRow(row);
            if (session != null && session.getSessionId() != null && !session.getSessionId().isEmpty()) {
                sessions.add(session);
            }
        }
        return sessions;
    }

    @Override
    public boolean update(String sessionId, AttendanceSession session) throws IOException {
        int rowIndex = googleSheetsService.findRowIndex(
                googleSheetsService.getDefaultSpreadsheetId(),
                SheetConstants.SHEET_ATTENDANCE_SESSIONS,
                0,
                sessionId
        );
        if (rowIndex == -1) {
            return false;
        }

        String range = SheetConstants.SHEET_ATTENDANCE_SESSIONS + "!A" + rowIndex + ":H" + rowIndex;
        googleSheetsService.updateRow(range, session.toRow());
        return true;
    }

    @Override
    public boolean delete(String sessionId) throws IOException {
        int rowIndex = googleSheetsService.findRowIndex(
                googleSheetsService.getDefaultSpreadsheetId(),
                SheetConstants.SHEET_ATTENDANCE_SESSIONS,
                0,
                sessionId
        );
        if (rowIndex == -1) {
            return false;
        }

        googleSheetsService.deleteRow(SheetConstants.SHEET_ATTENDANCE_SESSIONS, rowIndex);
        return true;
    }
}
