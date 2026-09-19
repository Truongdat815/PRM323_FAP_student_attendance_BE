package com.example.attendance_backend.config;

import com.example.attendance_backend.model.Role;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.RoleRepository;
import com.example.attendance_backend.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {
    PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository) {
        return application -> {
            // 1. Đảm bảo các Roles mặc định tồn tại trong bảng roles trước khi lưu User
            createRoleIfNotExist(roleRepository, "ROLE_ADMIN", "ADMIN", "Quản trị viên hệ thống");
            createRoleIfNotExist(roleRepository, "ADMIN", "ADMIN", "Quản trị viên hệ thống");
            createRoleIfNotExist(roleRepository, "ROLE_LECTURER", "LECTURER", "Giảng viên");
            createRoleIfNotExist(roleRepository, "LEADTURE", "LECTURER", "Giảng viên");
            createRoleIfNotExist(roleRepository, "ROLE_STUDENT", "STUDENT", "Sinh viên");

            // 2. Tạo tài khoản Admin mặc định
            if (userRepository.findByEmail("admin@gmail.com").isEmpty()) {
                String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                User user = User.builder()
                        .userId(userId)
                        .username("admin")
                        .email("admin@gmail.com")
                        .passwordHash(passwordEncoder.encode("Admin@1234"))
                        .fullName("Admin")
                        .roleId("ROLE_ADMIN")
                        .createdAt(LocalDateTime.now())
                        .build();
                userRepository.save(user);
                log.info("✅ Admin default user created");
            }

            // 3. Tạo tài khoản Lecturer mặc định
            if (userRepository.findByEmail("leadture@gmail.com").isEmpty()) {
                String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                User user = User.builder()
                        .userId(userId)
                        .username("leadture")
                        .email("leadture@gmail.com")
                        .passwordHash(passwordEncoder.encode("Leadture@1234"))
                        .fullName("Leadture")
                        .roleId("ROLE_LECTURER")
                        .createdAt(LocalDateTime.now())
                        .build();
                userRepository.save(user);
                log.info("✅ Lecturer default user created");
            }
        };
    }

    private void createRoleIfNotExist(RoleRepository roleRepository, String roleId, String roleName, String description) {
        if (roleRepository.findById(roleId).isEmpty()) {
            Role role = Role.builder()
                    .roleId(roleId)
                    .roleName(roleName)
                    .description(description)
                    .build();
            roleRepository.save(role);
            log.info("✅ Created default role: {}", roleId);
        }
    }
}
