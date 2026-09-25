package com.example.attendance_backend.service.impl;

import com.example.attendance_backend.dto.response.UserResponse;
import com.example.attendance_backend.exception.AppException;
import com.example.attendance_backend.model.Course;
import com.example.attendance_backend.model.Enrollment;
import com.example.attendance_backend.model.Role;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.CourseRepository;
import com.example.attendance_backend.repository.EnrollmentRepository;
import com.example.attendance_backend.repository.RoleRepository;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.service.ExcelImportService;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExcelImportServiceImpl implements ExcelImportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelImportServiceImpl.class);

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ExcelImportServiceImpl(UserRepository userRepository,
                                  CourseRepository courseRepository,
                                  EnrollmentRepository enrollmentRepository,
                                  RoleRepository roleRepository,
                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UserResponse> importUsersFromExcel(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "File Excel không được để trống.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Định dạng file không hợp lệ. Chỉ chấp nhận file .xlsx hoặc .xls.");
        }

        // Đảm bảo role ROLE_STUDENT tồn tại trong bảng Role
        String studentRoleId = "ROLE_STUDENT";
        if (roleRepository != null && roleRepository.findById(studentRoleId).isEmpty()) {
            Role studentRole = Role.builder()
                    .roleId(studentRoleId)
                    .roleName("STUDENT")
                    .description("Sinh viên")
                    .build();
            roleRepository.save(studentRole);
        }

        List<User> importedUsers = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getLastRowNum();

            for (int i = 1; i <= rowCount; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String className = formatter.formatCellValue(row.getCell(0)).trim();
                String rollNumber = formatter.formatCellValue(row.getCell(1)).trim();
                String email = formatter.formatCellValue(row.getCell(2)).trim();
                String memberCode = formatter.formatCellValue(row.getCell(3)).trim();
                String fullName = formatter.formatCellValue(row.getCell(4)).trim();
                String rawPassword = formatter.formatCellValue(row.getCell(5)).trim();

                // Skip completely empty rows
                if (rollNumber.isEmpty() && memberCode.isEmpty() && email.isEmpty() && fullName.isEmpty()) {
                    continue;
                }

                String username = !memberCode.isEmpty() ? memberCode : (!rollNumber.isEmpty() ? rollNumber : email);
                if (username.isEmpty()) {
                    continue;
                }

                String passwordToHash = !rawPassword.isEmpty() ? rawPassword : "A123456";
                String passwordHash = passwordEncoder.encode(passwordToHash);

                // Check if user already exists by username, memberCode, or fapCode
                Optional<User> existingUserOpt = userRepository.findByUsername(username);
                if (existingUserOpt.isEmpty() && !memberCode.isEmpty()) {
                    existingUserOpt = userRepository.findByMemberCode(memberCode);
                }
                if (existingUserOpt.isEmpty() && !rollNumber.isEmpty()) {
                    existingUserOpt = userRepository.findByFapCode(rollNumber);
                }

                User user;
                if (existingUserOpt.isPresent()) {
                    user = existingUserOpt.get();
                    user.setFapCode(rollNumber);
                    user.setEmail(email.toLowerCase());
                    user.setMemberCode(memberCode);
                    user.setFullName(fullName);
                    user.setRoleId(studentRoleId);
                    if (!rawPassword.isEmpty()) {
                        user.setPasswordHash(passwordHash);
                    }
                } else {
                    String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    user = User.builder()
                            .userId(userId)
                            .username(username)
                            .passwordHash(passwordHash)
                            .fapCode(rollNumber)
                            .fullName(fullName)
                            .email(email.toLowerCase())
                            .roleId(studentRoleId)
                            .createdAt(LocalDateTime.now())
                            .memberCode(memberCode)
                            .build();
                }

                User savedUser = userRepository.save(user);
                importedUsers.add(savedUser);

                // Lấy className từ bảng Course qua (không lưu ở User) và liên kết enrollment nếu tìm thấy Course tương ứng
                if (!className.isEmpty()) {
                    List<Course> courses = courseRepository.findByClassName(className);
                    for (Course course : courses) {
                        if (!enrollmentRepository.existsByCourseIdAndStudentId(course.getCourseId(), savedUser.getUserId())) {
                            Enrollment enrollment = Enrollment.builder()
                                    .enrollmentId("ENR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                                    .courseId(course.getCourseId())
                                    .studentId(savedUser.getUserId())
                                    .enrolledAt(LocalDateTime.now())
                                    .build();
                            enrollmentRepository.save(enrollment);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ Lỗi khi đọc file Excel import: ", e);
            throw new AppException(HttpStatus.BAD_REQUEST, "Lỗi đọc file Excel: " + e.getMessage());
        }

        log.info("✅ Đã import thành công {} người dùng từ file Excel: {}", importedUsers.size(), fileName);

        return importedUsers.stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }
}
