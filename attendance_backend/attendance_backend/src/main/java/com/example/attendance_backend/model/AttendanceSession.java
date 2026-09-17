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
public class AttendanceSession {

    private String sessionId;
    private String courseId;
    private Integer slotNumber;
    private String qrCodeSecret;
    private String roomNetworkIp;
    private String expiredAt;
    private String status;
    private String createdAt;

    public List<Object> toRow() {
        return Arrays.asList(
                sessionId != null ? sessionId : "",
                courseId != null ? courseId : "",
                slotNumber != null ? slotNumber.toString() : "",
                qrCodeSecret != null ? qrCodeSecret : "",
                roomNetworkIp != null ? roomNetworkIp : "",
                expiredAt != null ? expiredAt : "",
                status != null ? status : "",
                createdAt != null ? createdAt : ""
        );
    }

    public static AttendanceSession fromRow(List<Object> row) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        Integer slot = null;
        String slotStr = getCellValue(row, 2);
        if (!slotStr.isEmpty()) {
            try {
                slot = Integer.parseInt(slotStr);
            } catch (NumberFormatException e) {
                slot = null;
            }
        }
        return AttendanceSession.builder()
                .sessionId(getCellValue(row, 0))
                .courseId(getCellValue(row, 1))
                .slotNumber(slot)
                .qrCodeSecret(getCellValue(row, 3))
                .roomNetworkIp(getCellValue(row, 4))
                .expiredAt(getCellValue(row, 5))
                .status(getCellValue(row, 6))
                .createdAt(getCellValue(row, 7))
                .build();
    }

    private static String getCellValue(List<Object> row, int index) {
        if (index < row.size() && row.get(index) != null) {
            return row.get(index).toString().trim();
        }
        return "";
    }
}
