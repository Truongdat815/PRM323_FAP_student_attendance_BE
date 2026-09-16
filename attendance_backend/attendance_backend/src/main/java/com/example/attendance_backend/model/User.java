package com.example.attendance_backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Thực thể User ánh xạ tương ứng với dòng dữ liệu trong sheet USERS.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private String userId;
    private String username;
    private String passwordHash;
    private String fapCode;
    private String fullName;
    private String email;
    private String roleId;
    private String createdAt;

    /**
     * Chuyển đổi đối tượng User thành dòng dữ liệu ghi vào Google Sheet USERS.
     */
    public List<Object> toRow() {
        return Arrays.asList(
                userId != null ? userId : "",
                username != null ? username : "",
                passwordHash != null ? passwordHash : "",
                fapCode != null ? fapCode : "",
                fullName != null ? fullName : "",
                email != null ? email : "",
                roleId != null ? roleId : "",
                createdAt != null ? createdAt : ""
        );
    }

    /**
     * Ánh xạ một dòng dữ liệu từ Google Sheet USERS thành đối tượng User.
     */
    public static User fromRow(List<Object> row) {
        if (row == null || row.isEmpty()) {
            return null;
        }

        String userId = getCellValue(row, 0);
        String username = getCellValue(row, 1);
        String passwordHash = getCellValue(row, 2);
        String fapCode = getCellValue(row, 3);
        String fullName = getCellValue(row, 4);
        String email = getCellValue(row, 5);
        String roleId = getCellValue(row, 6);
        String createdAt = getCellValue(row, 7);

        return User.builder()
                .userId(userId)
                .username(username)
                .passwordHash(passwordHash)
                .fapCode(fapCode)
                .fullName(fullName)
                .email(email)
                .roleId(roleId)
                .createdAt(createdAt)
                .build();
    }

    private static String getCellValue(List<Object> row, int index) {
        if (index < row.size() && row.get(index) != null) {
            return row.get(index).toString().trim();
        }
        return "";
    }
}
