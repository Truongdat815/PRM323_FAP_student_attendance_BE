package com.example.attendance_backend.repository;

import com.example.attendance_backend.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, String> {

    List<AttendanceRecord> findBySessionId(String sessionId);

    List<AttendanceRecord> findByStudentId(String studentId);

    Optional<AttendanceRecord> findBySessionIdAndStudentId(String sessionId, String studentId);

    boolean existsBySessionIdAndStudentId(String sessionId, String studentId);

    boolean existsBySessionIdAndDeviceId(String sessionId, String deviceId);

    Optional<AttendanceRecord> findBySessionIdAndDeviceId(String sessionId, String deviceId);
}
