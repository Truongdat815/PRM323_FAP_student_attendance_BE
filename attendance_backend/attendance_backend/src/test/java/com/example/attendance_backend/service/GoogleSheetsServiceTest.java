package com.example.attendance_backend.service;

import com.example.attendance_backend.service.impl.GoogleSheetsServiceImpl;
import com.example.attendance_backend.util.SheetConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoogleSheetsServiceTest {

    @Test
    void testSheetConstants() {
        assertEquals("ROLES", SheetConstants.SHEET_ROLES);
        assertEquals("USERS", SheetConstants.SHEET_USERS);
        assertEquals("COURSES", SheetConstants.SHEET_COURSES);
        assertEquals("ENROLLMENTS", SheetConstants.SHEET_ENROLLMENTS);
        assertEquals("ATTENDANCE_SESSIONS", SheetConstants.SHEET_ATTENDANCE_SESSIONS);
        assertEquals("ATTENDANCE_RECORDS", SheetConstants.SHEET_ATTENDANCE_RECORDS);
        assertEquals("GOOGLE_SHEET_SYNC_LOGS", SheetConstants.SHEET_GOOGLE_SHEET_SYNC_LOGS);

        assertEquals("USERS!A2:Z", SheetConstants.dataRange(SheetConstants.SHEET_USERS));
        assertEquals("USERS!A:Z", SheetConstants.fullRange(SheetConstants.SHEET_USERS));
    }

    @Test
    void testServiceImplWithoutConnectionThrowsException() {
        GoogleSheetsService service = new GoogleSheetsServiceImpl(null);
        assertFalse(service.isConnected());
        assertThrows(IllegalStateException.class, () -> service.readRange("A1:B2"));
    }
}
