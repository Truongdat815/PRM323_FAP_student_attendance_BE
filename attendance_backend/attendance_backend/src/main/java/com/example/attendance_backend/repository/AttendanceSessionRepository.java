package com.example.attendance_backend.repository;

import com.example.attendance_backend.model.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, String> {

    List<AttendanceSession> findByCourseId(String courseId);

    Optional<AttendanceSession> findByQrCodeSecret(String qrCodeSecret);

    Optional<AttendanceSession> findBySessionCode(String sessionCode);

    List<AttendanceSession> findBySessionCodeAndStatus(String sessionCode, String status);

    long countByCourseIdIn(List<String> courseIds);
}
