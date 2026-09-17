package com.example.attendance_backend.repository;

import com.example.attendance_backend.model.AttendanceRecord;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository {

    AttendanceRecord save(AttendanceRecord record) throws IOException;

    Optional<AttendanceRecord> findById(String recordId) throws IOException;

    List<AttendanceRecord> findBySessionId(String sessionId) throws IOException;

    List<AttendanceRecord> findByStudentId(String studentId) throws IOException;

    boolean existsBySessionIdAndStudentId(String sessionId, String studentId) throws IOException;

    List<AttendanceRecord> findAll() throws IOException;
}
