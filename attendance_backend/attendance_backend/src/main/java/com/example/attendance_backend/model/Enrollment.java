package com.example.attendance_backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * Thực thể Enrollment tương ứng với dòng dữ liệu trên sheet ENROLLMENTS.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    private String enrollmentId;
    private String courseId;
    private String studentId;
    private String enrolledAt;

    public List<Object> toRow() {
        return Arrays.asList(
                enrollmentId != null ? enrollmentId : "",
                courseId != null ? courseId : "",
                studentId != null ? studentId : "",
                enrolledAt != null ? enrolledAt : ""
        );
    }

    public static Enrollment fromRow(List<Object> row) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        return Enrollment.builder()
                .enrollmentId(getCellValue(row, 0))
                .courseId(getCellValue(row, 1))
                .studentId(getCellValue(row, 2))
                .enrolledAt(getCellValue(row, 3))
                .build();
    }

    private static String getCellValue(List<Object> row, int index) {
        if (index < row.size() && row.get(index) != null) {
            return row.get(index).toString().trim();
        }
        return "";
    }
}
