package com.example.attendance_backend.repository;

import com.example.attendance_backend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {

    Optional<Role> findByRoleId(String roleId);

    Optional<Role> findByRoleName(String roleName);
}
