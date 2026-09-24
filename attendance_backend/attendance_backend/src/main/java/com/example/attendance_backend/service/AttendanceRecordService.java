package com.example.attendance_backend.service;

import com.example.attendance_backend.dto.request.CheckInRequest;
import com.example.attendance_backend.dto.response.AttendanceHistoryResponse;
import com.example.attendance_backend.dto.response.AttendanceRecordResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceRecordService {

    AttendanceRecordResponse checkIn(CheckInRequest request, String currentUsername, String clientIp) throws IOException;

    List<AttendanceRecordResponse> getRecordsBySession(String sessionId) throws IOException;

    byte[] exportAttendanceToCsv(String sessionId) throws IOException;

    List<AttendanceHistoryResponse> getMyAttendanceHistory(String currentUsername, String courseId, String subjectCode, LocalDate startDate, LocalDate endDate) throws IOException;

    byte[] exportAttendanceToExcel(String sessionId) throws IOException;
}
