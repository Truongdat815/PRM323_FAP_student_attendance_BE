package com.example.attendance_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đại diện cho thông tin tóm tắt của một khóa học gần đây trên Dashboard giảng viên.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentCourseResponse {

    private String courseId;
    private String subjectCode;
    private String className;
    private String semester;
    private long studentCount;
}
