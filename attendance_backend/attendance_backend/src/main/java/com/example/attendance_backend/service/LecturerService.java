package com.example.attendance_backend.service;

import com.example.attendance_backend.dto.response.LecturerDashboardStatsResponse;

import java.io.IOException;

/**
 * Interface nghiệp vụ các tính năng dành riêng cho Giảng viên.
 */
public interface LecturerService {

    /**
     * Lấy các số liệu thống kê Dashboard cho giảng viên đang đăng nhập.
     *
     * @param currentUsername username của tài khoản đang đăng nhập
     * @return {@link LecturerDashboardStatsResponse}
     * @throws IOException nếu có lỗi đọc dữ liệu
     */
    LecturerDashboardStatsResponse getDashboardStats(String currentUsername) throws IOException;

    /**
     * Lấy các số liệu thống kê Dashboard cho giảng viên đang đăng nhập hoặc theo lecturerId được chỉ định (Admin).
     *
     * @param currentUsername username của tài khoản đang đăng nhập
     * @param lecturerId      mã giảng viên tùy chọn (nếu có)
     * @return {@link LecturerDashboardStatsResponse}
     * @throws IOException nếu có lỗi đọc dữ liệu
     */
    LecturerDashboardStatsResponse getDashboardStats(String currentUsername, String lecturerId) throws IOException;
}
