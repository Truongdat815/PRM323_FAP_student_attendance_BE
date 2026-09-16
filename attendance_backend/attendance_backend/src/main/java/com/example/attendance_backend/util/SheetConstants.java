package com.example.attendance_backend.util;

/**
 * Định nghĩa tên các Sheet (Tab) và cấu trúc bảng trên Google Sheets Database.
 */
public final class SheetConstants {

    private SheetConstants() {
        // Private constructor to prevent instantiation
    }

    // Tên các Sheet (Tab) độc lập
    public static final String SHEET_ROLES = "ROLES";
    public static final String SHEET_USERS = "USERS";
    public static final String SHEET_COURSES = "COURSES";
    public static final String SHEET_ENROLLMENTS = "ENROLLMENTS";
    public static final String SHEET_ATTENDANCE_SESSIONS = "ATTENDANCE_SESSIONS";
    public static final String SHEET_ATTENDANCE_RECORDS = "ATTENDANCE_RECORDS";
    public static final String SHEET_GOOGLE_SHEET_SYNC_LOGS = "GOOGLE_SHEET_SYNC_LOGS";

    // Header mặc định cho các Sheet khi khởi tạo
    public static final String[] HEADERS_ROLES = {
            "role_id", "role_name", "description"
    };

    public static final String[] HEADERS_USERS = {
            "user_id", "username", "password_hash", "fap_code", "full_name", "email", "role_id", "created_at"
    };

    public static final String[] HEADERS_COURSES = {
            "course_id", "subject_code", "class_name", "semester", "lecturer_id", "created_at"
    };

    public static final String[] HEADERS_ENROLLMENTS = {
            "enrollment_id", "course_id", "student_id", "enrolled_at"
    };

    public static final String[] HEADERS_ATTENDANCE_SESSIONS = {
            "session_id", "course_id", "slot_number", "qr_code_secret", "room_network_ip", "expired_at", "status", "created_at"
    };

    public static final String[] HEADERS_ATTENDANCE_RECORDS = {
            "record_id", "session_id", "student_id", "status", "checked_at"
    };

    public static final String[] HEADERS_GOOGLE_SHEET_SYNC_LOGS = {
            "log_id", "action", "status", "details", "synced_at"
    };

    // Dải ô mặc định bỏ qua Header dòng 1 (A2:Z)
    public static String dataRange(String sheetName) {
        return sheetName + "!A2:Z";
    }

    public static String fullRange(String sheetName) {
        return sheetName + "!A:Z";
    }
}
