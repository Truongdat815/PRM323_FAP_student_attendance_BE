package com.example.attendance_backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * Thực thể Course tương ứng với dòng dữ liệu trên sheet COURSES.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    private String courseId;
    private String subjectCode;
    private String className;
    private String semester;
    private String lecturerId;
    private String createdAt;

    public List<Object> toRow() {
        return Arrays.asList(
                courseId != null ? courseId : "",
                subjectCode != null ? subjectCode : "",
                className != null ? className : "",
                semester != null ? semester : "",
                lecturerId != null ? lecturerId : "",
                createdAt != null ? createdAt : ""
        );
    }

    public static Course fromRow(List<Object> row) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        return Course.builder()
                .courseId(getCellValue(row, 0))
                .subjectCode(getCellValue(row, 1))
                .className(getCellValue(row, 2))
                .semester(getCellValue(row, 3))
                .lecturerId(getCellValue(row, 4))
                .createdAt(getCellValue(row, 5))
                .build();
    }

    private static String getCellValue(List<Object> row, int index) {
        if (index < row.size() && row.get(index) != null) {
            return row.get(index).toString().trim();
        }
        return "";
    }
}
