package com.example.attendance_backend.service;

import com.example.attendance_backend.dto.request.SessionCreateRequest;
import com.example.attendance_backend.dto.response.SessionResponse;

import java.io.IOException;
import java.util.List;

public interface AttendanceSessionService {

    SessionResponse createSession(SessionCreateRequest request, String currentUsername) throws IOException;

    SessionResponse getSessionById(String sessionId) throws IOException;

    List<SessionResponse> getSessionsByCourse(String courseId) throws IOException;

    SessionResponse closeSession(String sessionId, String currentUsername) throws IOException;
}
