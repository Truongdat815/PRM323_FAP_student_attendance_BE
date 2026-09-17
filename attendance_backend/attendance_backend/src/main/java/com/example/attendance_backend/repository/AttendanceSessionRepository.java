package com.example.attendance_backend.repository;

import com.example.attendance_backend.model.AttendanceSession;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface AttendanceSessionRepository {

    AttendanceSession save(AttendanceSession session) throws IOException;

    Optional<AttendanceSession> findById(String sessionId) throws IOException;

    List<AttendanceSession> findByCourseId(String courseId) throws IOException;

    List<AttendanceSession> findAll() throws IOException;

    boolean update(String sessionId, AttendanceSession session) throws IOException;

    boolean delete(String sessionId) throws IOException;
}
