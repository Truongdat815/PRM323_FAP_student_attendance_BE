package com.example.attendance_backend.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

import com.example.attendance_backend.dto.request.CourseRegisterRequest;
import com.example.attendance_backend.dto.request.CourseRequest;
import com.example.attendance_backend.dto.response.CourseResponse;
import com.example.attendance_backend.dto.response.ImportStudentsResponse;
import com.example.attendance_backend.exception.AppException;
import com.example.attendance_backend.model.Course;
import com.example.attendance_backend.model.Enrollment;
import com.example.attendance_backend.model.User;
import com.example.attendance_backend.repository.CourseRepository;
import com.example.attendance_backend.repository.EnrollmentRepository;
import com.example.attendance_backend.repository.UserRepository;
import com.example.attendance_backend.service.CourseService;

/**
 * Triển khai nghiệp vụ CRUD khóa học cho Giảng viên.
 */
@Service
public class CourseServiceImpl implements CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public CourseServiceImpl(CourseRepository courseRepository,
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository,
            PasswordEncoder passwordEncoder) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public CourseServiceImpl(CourseRepository courseRepository, UserRepository userRepository) {
        this(courseRepository, userRepository, null, null);
    }

    @Override
    public List<CourseResponse> getAllCourses(String lecturerId) throws IOException {
        List<Course> courses;
        if (lecturerId != null && !lecturerId.trim().isEmpty()) {
            courses = courseRepository.findByLecturerId(lecturerId.trim());
        } else {
            courses = courseRepository.findAll();
        }

        return courses.stream()
                .map(CourseResponse::fromCourse)
                .collect(Collectors.toList());
    }

    @Override
    public CourseResponse getCourseById(String courseId) throws IOException {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với mã: " + courseId));
        return CourseResponse.fromCourse(course);
    }

    @Override
    public CourseResponse createCourse(CourseRequest request, String currentUsername) throws IOException {
        User currentUser = getCurrentUser(currentUsername);

        // Kiểm tra quyền hạn: phải là LECTURER hoặc ADMIN
        if (!"ROLE_LECTURER".equalsIgnoreCase(currentUser.getRoleId()) && !"ROLE_ADMIN".equalsIgnoreCase(currentUser.getRoleId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Chỉ giảng viên mới có quyền tạo khóa học mới.");
        }

        String lecturerId = (request.getLecturerId() != null && !request.getLecturerId().trim().isEmpty())
                ? request.getLecturerId().trim()
                : currentUser.getUserId();

        String encodedPassword = null;
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            encodedPassword = (passwordEncoder != null)
                    ? passwordEncoder.encode(request.getPassword().trim())
                    : request.getPassword().trim();
        }

        String courseId = "CRS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Course newCourse = Course.builder()
                .courseId(courseId)
                .subjectCode(request.getSubjectCode().trim().toUpperCase())
                .className(request.getClassName().trim().toUpperCase())
                .semester(request.getSemester().trim().toUpperCase())
                .lecturerId(lecturerId)
                .password(encodedPassword)
                .createdAt(LocalDateTime.now())
                .build();

        courseRepository.save(newCourse);
        log.info("✅ Giảng viên '{}' đã tạo khóa học thành công: courseId={}, subjectCode={}, class={}",
                currentUsername, courseId, newCourse.getSubjectCode(), newCourse.getClassName());

        return CourseResponse.fromCourse(newCourse);
    }

    @Override
    public CourseResponse updateCourse(String courseId, CourseRequest request, String currentUsername) throws IOException {
        Course existingCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với mã: " + courseId));

        User currentUser = getCurrentUser(currentUsername);

        // Kiểm tra quyền: chỉ giảng viên tạo khóa học hoặc admin mới được sửa
        if (!currentUser.getUserId().equalsIgnoreCase(existingCourse.getLecturerId())
                && !"ROLE_ADMIN".equalsIgnoreCase(currentUser.getRoleId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền chỉnh sửa khóa học do giảng viên khác phụ trách.");
        }

        existingCourse.setSubjectCode(request.getSubjectCode().trim().toUpperCase());
        existingCourse.setClassName(request.getClassName().trim().toUpperCase());
        existingCourse.setSemester(request.getSemester().trim().toUpperCase());
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            existingCourse.setPassword((passwordEncoder != null)
                    ? passwordEncoder.encode(request.getPassword().trim())
                    : request.getPassword().trim());
        }

        Course updatedCourse = courseRepository.save(existingCourse);

        log.info("✅ Cập nhật thông tin khóa học thành công: courseId={}", courseId);
        return CourseResponse.fromCourse(updatedCourse);
    }

    @Override
    public void deleteCourse(String courseId, String currentUsername) throws IOException {
        Course existingCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với mã: " + courseId));

        User currentUser = getCurrentUser(currentUsername);

        // Kiểm tra quyền: chỉ giảng viên tạo khóa học hoặc admin mới được xóa
        if (!currentUser.getUserId().equalsIgnoreCase(existingCourse.getLecturerId())
                && !"ROLE_ADMIN".equalsIgnoreCase(currentUser.getRoleId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa khóa học do giảng viên khác phụ trách.");
        }

        courseRepository.delete(existingCourse);

        log.info("✅ Giảng viên '{}' đã xóa khóa học: courseId={}", currentUsername, courseId);
    }

    @Override
    public CourseResponse registerCourse(CourseRegisterRequest request, String currentUsername) throws IOException {
        String targetUserId = (request.getUserId() != null && !request.getUserId().trim().isEmpty())
                ? request.getUserId().trim()
                : currentUsername;

        User student = userRepository.findById(targetUserId)
                .or(() -> userRepository.findByUsername(targetUserId))
                .or(() -> userRepository.findByFapCode(targetUserId))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học sinh với mã: " + targetUserId));

        // Kiểm tra vai trò: Chỉ sinh viên (hoặc admin) mới được đăng ký
        String role = student.getRoleId();
        boolean isStudent = role != null && (role.equalsIgnoreCase("STUDENT") || role.equalsIgnoreCase("ROLE_STUDENT"));
        boolean isAdmin = role != null && (role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("ROLE_ADMIN"));
        if (!isStudent && !isAdmin) {
            throw new AppException(HttpStatus.FORBIDDEN, "Chỉ tài khoản sinh viên mới có quyền đăng ký khóa học.");
        }

        String courseId = request.getCourseId() != null ? request.getCourseId().trim() : "";
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với mã: " + courseId));

        // Kiểm tra mật khẩu khóa học (nếu khóa học có thiết lập mật khẩu)
        if (course.getPassword() != null && !course.getPassword().trim().isEmpty()) {
            String expectedPassword = course.getPassword().trim();
            String providedPassword = (request.getCoursePassword() != null) ? request.getCoursePassword().trim() : "";

            boolean matches = expectedPassword.equals(providedPassword);
            if (!matches && passwordEncoder != null) {
                matches = passwordEncoder.matches(providedPassword, expectedPassword);
            }

            if (!matches) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Mật khẩu khóa học không chính xác.");
            }
        }

        // Kiểm tra đăng ký trùng lặp
        if (enrollmentRepository.existsByCourseIdAndStudentId(course.getCourseId(), student.getUserId())) {
            throw new AppException(HttpStatus.CONFLICT, "Học sinh đã đăng ký tham gia khóa học này trước đó.");
        }

        // Tạo bản ghi Enrollment
        String enrollmentId = "ENR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Enrollment enrollment = Enrollment.builder()
                .enrollmentId(enrollmentId)
                .courseId(course.getCourseId())
                .studentId(student.getUserId())
                .enrolledAt(LocalDateTime.now())
                .build();
        enrollmentRepository.save(enrollment);

        log.info("✅ Học sinh '{}' (userId={}) đăng ký thành công khóa học: courseId={}, className={}",
                student.getUsername(), student.getUserId(), course.getCourseId(), course.getClassName());

        // Lấy tên giảng viên nếu có
        String lecturerName = null;
        if (course.getLecturerId() != null) {
            lecturerName = userRepository.findById(course.getLecturerId())
                    .map(User::getFullName)
                    .orElse(null);
        }

        return CourseResponse.fromCourse(course, lecturerName);
    }

    @Override
    public List<CourseResponse> searchCourses(String className, String subjectCode, String semester, String lecturerName) throws IOException {
        return searchCourses(className, subjectCode, semester, lecturerName, null);
    }

    @Override
    public List<CourseResponse> searchCourses(String className, String subjectCode, String semester, String lecturerName, String lecturerId) throws IOException {
        String cleanClassName = (className != null && !className.trim().isEmpty()) ? className.trim() : null;
        String cleanSubjectCode = (subjectCode != null && !subjectCode.trim().isEmpty()) ? subjectCode.trim() : null;
        String cleanSemester = (semester != null && !semester.trim().isEmpty()) ? semester.trim() : null;
        String cleanLecturerName = (lecturerName != null && !lecturerName.trim().isEmpty()) ? lecturerName.trim() : null;
        String cleanLecturerId = (lecturerId != null && !lecturerId.trim().isEmpty()) ? lecturerId.trim() : null;

        List<Course> courses = courseRepository.searchCourses(cleanClassName, cleanSubjectCode, cleanSemester, cleanLecturerName, cleanLecturerId);

        Set<String> lecturerIds = courses.stream()
                .map(Course::getLecturerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, String> lecturerMap = new HashMap<>();
        if (!lecturerIds.isEmpty()) {
            lecturerMap = userRepository.findAllById(lecturerIds).stream()
                    .collect(Collectors.toMap(User::getUserId, User::getFullName, (oldVal, newVal) -> oldVal));
        }

        Map<String, String> finalLecturerMap = lecturerMap;
        return courses.stream()
                .map(c -> CourseResponse.fromCourse(c, finalLecturerMap.get(c.getLecturerId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponse> getEnrolledCourses(String studentId) throws IOException {
        return getEnrolledCourses(studentId, null, null, null);
    }

    @Override
    public List<CourseResponse> getEnrolledCourses(String studentId, String subjectCode, String semester, String lecturerName) throws IOException {
        // Tìm học sinh
        User student = userRepository.findById(studentId)
                .or(() -> userRepository.findByUsername(studentId))
                .or(() -> userRepository.findByFapCode(studentId))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học sinh với mã: " + studentId));

        // Lấy danh sách enrollment của học sinh
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getUserId());
        if (enrollments.isEmpty()) {
            return Collections.emptyList();
        }

        // Lấy danh sách courseId từ enrollments
        List<String> courseIds = enrollments.stream()
                .map(Enrollment::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Batch-fetch tất cả courses
        List<Course> courses = courseRepository.findAllById(courseIds);

        // Batch-fetch tất cả tên giảng viên
        Set<String> lecturerIds = courses.stream()
                .map(Course::getLecturerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, String> lecturerMap = new HashMap<>();
        if (!lecturerIds.isEmpty()) {
            lecturerMap = userRepository.findAllById(lecturerIds).stream()
                    .collect(Collectors.toMap(User::getUserId, User::getFullName, (oldVal, newVal) -> oldVal));
        }

        Map<String, String> finalLecturerMap = lecturerMap;

        // Áp dụng bộ lọc tùy chọn
        return courses.stream()
                .filter(c -> {
                    if (subjectCode != null && !subjectCode.trim().isEmpty()) {
                        if (c.getSubjectCode() == null || !c.getSubjectCode().toLowerCase().contains(subjectCode.trim().toLowerCase())) {
                            return false;
                        }
                    }
                    if (semester != null && !semester.trim().isEmpty()) {
                        if (c.getSemester() == null || !c.getSemester().equalsIgnoreCase(semester.trim())) {
                            return false;
                        }
                    }
                    if (lecturerName != null && !lecturerName.trim().isEmpty()) {
                        String name = finalLecturerMap.get(c.getLecturerId());
                        if (name == null || !name.toLowerCase().contains(lecturerName.trim().toLowerCase())) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(c -> CourseResponse.fromCourse(c, finalLecturerMap.get(c.getLecturerId())))
                .collect(Collectors.toList());
    }

    @Override
    public ImportStudentsResponse importStudentsToCourse(String courseId, MultipartFile file, String currentUsername) throws IOException {
        User currentUser = getCurrentUser(currentUsername);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học với mã: " + courseId));

        if (!currentUser.getUserId().equalsIgnoreCase(course.getLecturerId()) && !"ROLE_ADMIN".equalsIgnoreCase(currentUser.getRoleId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền import sinh viên cho khóa học do giảng viên khác phụ trách.");
        }

        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "File Excel không được để trống.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Định dạng file không hợp lệ. Chỉ chấp nhận file .xlsx hoặc .xls.");
        }

        String studentRoleId = "ROLE_STUDENT";
        DataFormatter formatter = new DataFormatter();
        int successCount = 0;
        int skippedCount = 0;
        int totalRows = 0;
        List<String> errors = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            totalRows = sheet.getLastRowNum();

            // Nhận diện cột theo tiêu đề Header dòng 0
            Row headerRow = sheet.getRow(0);
            int rollNumberCol = -1;
            int emailCol = -1;
            int memberCodeCol = -1;
            int fullNameCol = -1;

            if (headerRow != null) {
                for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                    String h = formatter.formatCellValue(headerRow.getCell(c)).trim().toLowerCase();
                    if (h.contains("roll") || h.contains("fap") || h.contains("mssv") || h.contains("student")) {
                        rollNumberCol = c;
                    } else if (h.contains("email") || h.contains("mail")) {
                        emailCol = c;
                    } else if (h.contains("member") || h.contains("user")) {
                        memberCodeCol = c;
                    } else if (h.contains("name") || h.contains("họ") || h.contains("ten")) {
                        fullNameCol = c;
                    }
                }
            }

            if (rollNumberCol == -1) {
                rollNumberCol = 1;
            }
            if (emailCol == -1) {
                emailCol = 2;
            }
            if (memberCodeCol == -1) {
                memberCodeCol = 3;
            }
            if (fullNameCol == -1) {
                fullNameCol = 4;
            }

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String rollNumber = rollNumberCol >= 0 ? formatter.formatCellValue(row.getCell(rollNumberCol)).trim() : "";
                String email = emailCol >= 0 ? formatter.formatCellValue(row.getCell(emailCol)).trim() : "";
                String memberCode = memberCodeCol >= 0 ? formatter.formatCellValue(row.getCell(memberCodeCol)).trim() : "";
                String fullName = fullNameCol >= 0 ? formatter.formatCellValue(row.getCell(fullNameCol)).trim() : "";

                if (rollNumber.isEmpty() && email.isEmpty() && memberCode.isEmpty()) {
                    continue;
                }

                try {
                    // Tìm kiếm sinh viên
                    Optional<User> studentOpt = Optional.empty();

                    if (!rollNumber.isEmpty()) {
                        studentOpt = userRepository.findByFapCode(rollNumber);
                    }
                    if (studentOpt.isEmpty() && !email.isEmpty()) {
                        studentOpt = userRepository.findByEmail(email.toLowerCase());
                    }
                    if (studentOpt.isEmpty() && !memberCode.isEmpty()) {
                        studentOpt = userRepository.findByMemberCode(memberCode)
                                .or(() -> userRepository.findByUsername(memberCode));
                    }

                    User student;
                    if (studentOpt.isPresent()) {
                        student = studentOpt.get();
                    } else {
                        // Tự động tạo tài khoản sinh viên mặc định nếu chưa có
                        String username = !memberCode.isEmpty() ? memberCode : (!rollNumber.isEmpty() ? rollNumber : email);
                        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                        student = User.builder()
                                .userId(userId)
                                .username(username)
                                .fapCode(!rollNumber.isEmpty() ? rollNumber : null)
                                .email(!email.isEmpty() ? email.toLowerCase() : null)
                                .memberCode(!memberCode.isEmpty() ? memberCode : null)
                                .fullName(!fullName.isEmpty() ? fullName : username)
                                .roleId(studentRoleId)
                                .passwordHash(passwordEncoder != null ? passwordEncoder.encode("A123456") : "A123456")
                                .createdAt(LocalDateTime.now())
                                .build();
                        userRepository.save(student);
                    }

                    // Ghi danh vào khóa học nếu chưa tồn tại
                    if (!enrollmentRepository.existsByCourseIdAndStudentId(course.getCourseId(), student.getUserId())) {
                        String enrollmentId = "ENR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                        Enrollment enrollment = Enrollment.builder()
                                .enrollmentId(enrollmentId)
                                .courseId(course.getCourseId())
                                .studentId(student.getUserId())
                                .enrolledAt(LocalDateTime.now())
                                .build();
                        enrollmentRepository.save(enrollment);
                        successCount++;
                    } else {
                        skippedCount++;
                    }
                } catch (Exception ex) {
                    errors.add("Dòng " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }

        return ImportStudentsResponse.builder()
                .courseId(course.getCourseId())
                .totalRows(totalRows)
                .successCount(successCount)
                .skippedCount(skippedCount)
                .errors(errors)
                .build();
    }

    private User getCurrentUser(String username) throws IOException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Người dùng hiện tại không tồn tại trong hệ thống."));
    }
}
