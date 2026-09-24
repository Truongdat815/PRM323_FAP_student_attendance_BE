package com.example.attendance_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO trả về số liệu thống kê Dashboard tổng quan cho Giảng viên.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerDashboardStatsResponse {

    private String lecturerId;
    private long totalCourses;
    private long totalRegisteredStudents;
    private int activeSemestersCount;
    private List<String> activeSemesters;
    private long totalSessions;
    private List<RecentCourseResponse> recentCourses;
}
