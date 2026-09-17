package com.example.attendance_backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {

    private String recordId;
    private String sessionId;
    private String studentId;
    private String status;
    private String checkedAt;

    public List<Object> toRow() {
        return Arrays.asList(
                recordId != null ? recordId : "",
                sessionId != null ? sessionId : "",
                studentId != null ? studentId : "",
                status != null ? status : "",
                checkedAt != null ? checkedAt : ""
        );
    }

    public static AttendanceRecord fromRow(List<Object> row) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        return AttendanceRecord.builder()
                .recordId(getCellValue(row, 0))
                .sessionId(getCellValue(row, 1))
                .studentId(getCellValue(row, 2))
                .status(getCellValue(row, 3))
                .checkedAt(getCellValue(row, 4))
                .build();
    }

    private static String getCellValue(List<Object> row, int index) {
        if (index < row.size() && row.get(index) != null) {
            return row.get(index).toString().trim();
        }
        return "";
    }
}
