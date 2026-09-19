package com.example.attendance_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * Thực thể Role ánh xạ tương ứng với bảng roles.
 */
@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @Column(name = "role_id", nullable = false)
    private String roleId;

    @Column(name = "role_name")
    private String roleName;
    @Column(name = "description")
    private String description;
}
