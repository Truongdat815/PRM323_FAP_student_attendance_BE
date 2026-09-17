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
public class GoogleSheetSyncLog {

    private String logId;
    private String action;
    private String status;
    private String details;
    private String syncedAt;

    public List<Object> toRow() {
        return Arrays.asList(
                logId != null ? logId : "",
                action != null ? action : "",
                status != null ? status : "",
                details != null ? details : "",
                syncedAt != null ? syncedAt : ""
        );
    }

    public static GoogleSheetSyncLog fromRow(List<Object> row) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        return GoogleSheetSyncLog.builder()
                .logId(getCellValue(row, 0))
                .action(getCellValue(row, 1))
                .status(getCellValue(row, 2))
                .details(getCellValue(row, 3))
                .syncedAt(getCellValue(row, 4))
                .build();
    }

    private static String getCellValue(List<Object> row, int index) {
        if (index < row.size() && row.get(index) != null) {
            return row.get(index).toString().trim();
        }
        return "";
    }
}
