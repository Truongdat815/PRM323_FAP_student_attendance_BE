package com.example.attendance_backend.service;

import com.example.attendance_backend.dto.request.CheckInRequest;
import com.example.attendance_backend.dto.response.AttendanceRecordResponse;

import java.io.IOException;
import java.util.List;

public interface AttendanceRecordService {

    AttendanceRecordResponse checkIn(CheckInRequest request, String currentUsername, String clientIp) throws IOException;

    List<AttendanceRecordResponse> getRecordsBySession(String sessionId) throws IOException;

    byte[] exportAttendanceToCsv(String sessionId) throws IOException;
}
