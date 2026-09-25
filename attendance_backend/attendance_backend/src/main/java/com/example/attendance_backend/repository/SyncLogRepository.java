package com.example.attendance_backend.repository;

import com.example.attendance_backend.model.GoogleSheetSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncLogRepository extends JpaRepository<GoogleSheetSyncLog, String> {

    List<GoogleSheetSyncLog> findByStatus(String status);
}
