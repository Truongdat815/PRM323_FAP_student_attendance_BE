package com.example.attendance_backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * Thực thể Role ánh xạ tương ứng với sheet ROLES.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    private String roleId;
    private String roleName;
    private String description;

    public List<Object> toRow() {
        return Arrays.asList(
                roleId != null ? roleId : "",
                roleName != null ? roleName : "",
                description != null ? description : ""
        );
    }

    public static Role fromRow(List<Object> row) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        return Role.builder()
                .roleId(row.size() > 0 && row.get(0) != null ? row.get(0).toString().trim() : "")
                .roleName(row.size() > 1 && row.get(1) != null ? row.get(1).toString().trim() : "")
                .description(row.size() > 2 && row.get(2) != null ? row.get(2).toString().trim() : "")
                .build();
    }
}
