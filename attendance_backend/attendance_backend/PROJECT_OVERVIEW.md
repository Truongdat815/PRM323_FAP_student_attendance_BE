# 📚 FAP Student Attendance System — Backend

> Hệ thống điểm danh sinh viên FPT dựa trên QR Code, xây dựng bằng **Spring Boot 4.1** + **MySQL** + **Google Sheets** (dual-database).

---

## 1. Tổng quan dự án

**FAP Student Attendance System** là hệ thống backend RESTful API phục vụ việc điểm danh sinh viên tại trường Đại học FPT. Hệ thống hỗ trợ:

- **Giảng viên** tạo buổi học, sinh mã QR điểm danh, quản lý khóa học và theo dõi sinh viên.
- **Sinh viên** đăng ký khóa học, quét mã QR để điểm danh.
- **Admin** quản lý toàn bộ hệ thống, import dữ liệu từ file Excel.

### Quy trình nghiệp vụ chính

```
Giảng viên tạo khóa học → Sinh viên đăng ký khóa học
    → Giảng viên tạo buổi điểm danh (sinh QR Code)
        → Sinh viên quét QR để điểm danh
            → Xuất báo cáo CSV / Đồng bộ Google Sheets
```

---

## 2. Công nghệ sử dụng (Tech Stack)

| Thành phần          | Công nghệ                                        |
|---------------------|---------------------------------------------------|
| **Framework**       | Spring Boot 4.1.1                                 |
| **Ngôn ngữ**        | Java 21                                           |
| **Build Tool**      | Maven (Maven Wrapper)                             |
| **Database chính**  | MySQL (JPA / Hibernate)                           |
| **Database phụ**    | Google Sheets API v4 (dual-database)              |
| **Xác thực**        | JWT (JSON Web Token) — `jjwt 0.11.5`              |
| **Bảo mật**         | Spring Security 6.x, BCrypt password hashing      |
| **API Docs**        | Swagger UI / OpenAPI 3 — `springdoc-openapi 3.1.0`|
| **QR Code**         | Google ZXing 3.5.3                                |
| **Import Excel**    | Apache POI 5.2.5                                  |
| **Lombok**          | Lombok (giảm boilerplate code)                    |

---

## 3. Cấu trúc thư mục dự án

```
attendance_backend/
├── pom.xml                          # Maven dependencies & build config
├── mvnw / mvnw.cmd                  # Maven Wrapper (cross-platform)
└── src/
    └── main/
        ├── java/com/example/attendance_backend/
        │   ├── AttendanceBackendApplication.java   # Entry point (Main class)
        │   │
        │   ├── config/                             # ⚙️ Cấu hình hệ thống
        │   │   ├── GoogleSheetsConfig.java         # Khởi tạo Google Sheets client
        │   │   ├── OpenApiConfig.java              # Cấu hình Swagger UI
        │   │   ├── SecurityConfig.java             # Spring Security filter chain
        │   │   └── SecurityBeansConfig.java        # Bean PasswordEncoder (BCrypt)
        │   │
        │   ├── controller/                         # 🎯 REST API Controllers
        │   │   ├── AuthController.java             # Đăng ký / Đăng nhập
        │   │   ├── CourseController.java            # CRUD Khóa học
        │   │   ├── EnrollmentController.java        # Đăng ký khóa học
        │   │   ├── AttendanceSessionController.java # Quản lý buổi điểm danh
        │   │   ├── AttendanceController.java        # Điểm danh & xuất CSV
        │   │   ├── UserController.java              # Import user từ Excel
        │   │   └── SyncLogController.java           # Nhật ký đồng bộ
        │   │
        │   ├── dto/                                # 📦 Data Transfer Objects
        │   │   ├── request/                        # Input DTOs (Client → Server)
        │   │   │   ├── LoginRequest.java
        │   │   │   ├── RegisterRequest.java
        │   │   │   ├── CourseRequest.java
        │   │   │   ├── EnrollmentRequest.java
        │   │   │   ├── SessionCreateRequest.java
        │   │   │   └── CheckInRequest.java
        │   │   └── response/                       # Output DTOs (Server → Client)
        │   │       ├── ApiResponse.java            # Cấu trúc response chuẩn
        │   │       ├── AuthResponse.java
        │   │       ├── UserResponse.java
        │   │       ├── CourseResponse.java
        │   │       ├── EnrollmentResponse.java
        │   │       ├── SessionResponse.java
        │   │       ├── AttendanceRecordResponse.java
        │   │       └── SyncLogResponse.java
        │   │
        │   ├── model/                              # 🗄️ JPA Entities (Database Tables)
        │   │   ├── User.java                       # Bảng users
        │   │   ├── Role.java                       # Bảng roles
        │   │   ├── Course.java                     # Bảng courses
        │   │   ├── Enrollment.java                 # Bảng enrollments
        │   │   ├── AttendanceSession.java          # Bảng attendance_sessions
        │   │   ├── AttendanceRecord.java           # Bảng attendance_records
        │   │   └── GoogleSheetSyncLog.java         # Bảng google_sheet_sync_logs
        │   │
        │   ├── repository/                         # 🔍 Spring Data JPA Repositories
        │   │   ├── UserRepository.java
        │   │   ├── RoleRepository.java
        │   │   ├── CourseRepository.java
        │   │   ├── EnrollmentRepository.java
        │   │   ├── AttendanceSessionRepository.java
        │   │   ├── AttendanceRecordRepository.java
        │   │   └── SyncLogRepository.java
        │   │
        │   ├── service/                            # 💼 Business Logic Layer
        │   │   ├── AuthService.java                # Interface xác thực
        │   │   ├── CourseService.java              # Interface khóa học
        │   │   ├── EnrollmentService.java          # Interface đăng ký
        │   │   ├── AttendanceSessionService.java   # Interface buổi điểm danh
        │   │   ├── AttendanceRecordService.java    # Interface điểm danh
        │   │   ├── ExcelImportService.java         # Interface import Excel
        │   │   ├── GoogleSheetsService.java        # Interface Google Sheets
        │   │   ├── SyncLogService.java             # Interface nhật ký
        │   │   └── impl/                           # Implementations
        │   │       ├── AuthServiceImpl.java
        │   │       ├── CourseServiceImpl.java
        │   │       ├── EnrollmentServiceImpl.java
        │   │       ├── AttendanceSessionServiceImpl.java
        │   │       ├── AttendanceRecordServiceImpl.java
        │   │       ├── ExcelImportServiceImpl.java
        │   │       ├── GoogleSheetsServiceImpl.java
        │   │       └── SyncLogServiceImpl.java
        │   │
        │   ├── security/                           # 🔒 JWT & Spring Security
        │   │   ├── JwtTokenProvider.java           # Sinh/Giải mã/Xác thực JWT
        │   │   ├── JwtAuthenticationFilter.java    # Filter chặn mọi request
        │   │   ├── JwtAuthenticationEntryPoint.java# Xử lý 401 Unauthorized
        │   │   ├── CustomUserDetails.java          # Adapter UserDetails
        │   │   └── CustomUserDetailsService.java   # Load user từ DB
        │   │
        │   ├── exception/                          # ❌ Xử lý lỗi tập trung
        │   │   ├── AppException.java               # Custom business exception
        │   │   └── GlobalExceptionHandler.java     # @RestControllerAdvice
        │   │
        │   └── util/                               # 🛠️ Tiện ích
        │       ├── QRCodeGenerator.java            # Sinh QR Code base64
        │       └── SheetConstants.java             # Tên sheet & header constants
        │
        └── resources/
            └── application.properties              # Cấu hình ứng dụng
```

---

## 4. Kiến trúc hệ thống (Architecture)

Dự án sử dụng kiến trúc **Layered Architecture** theo chuẩn Spring Boot:

```
┌───────────────────────────────────────────────┐
│                  CLIENT (Mobile / Web)         │
│          (gửi request kèm JWT Token)          │
└──────────────────┬────────────────────────────┘
                   │ HTTP Request
                   ▼
┌──────────────────────────────────────────────┐
│          Controller Layer (REST APIs)         │
│  Nhận request, validate input, trả response  │
└──────────────────┬───────────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────────┐
│          Service Layer (Business Logic)       │
│  Xử lý nghiệp vụ, rules, validation logic   │
└──────────┬───────────────────┬───────────────┘
           │                   │
           ▼                   ▼
┌──────────────────┐  ┌──────────────────────────┐
│ Repository Layer │  │ Google Sheets Service     │
│  (JPA / MySQL)   │  │ (Dual-Database Sync)      │
└──────┬───────────┘  └──────────┬───────────────┘
       │                         │
       ▼                         ▼
   ┌────────┐           ┌─────────────────┐
   │ MySQL  │           │ Google Sheets   │
   │   DB   │           │   Spreadsheet   │
   └────────┘           └─────────────────┘
```

### Luồng xử lý Request

1. Client gửi HTTP Request → **JwtAuthenticationFilter** kiểm tra JWT Token
2. Nếu token hợp lệ → Request đi tới **Controller**
3. Controller validate input (@Valid) → Gọi **Service**
4. Service xử lý nghiệp vụ → Gọi **Repository** (MySQL) + **GoogleSheetsService** (Google Sheets)
5. Trả về **ApiResponse** chuẩn cho Client

---

## 5. Cơ sở dữ liệu (Database Schema)

### 5.1. Lược đồ quan hệ (ER Diagram)

```
┌──────────┐      ┌───────────┐      ┌───────────────────┐
│  roles   │◄─────│   users   │◄─────│   enrollments     │
│──────────│      │───────────│      │───────────────────│
│ role_id  │      │ user_id   │      │ enrollment_id     │
│ role_name│      │ username  │      │ course_id  ──────►│┐
│ descript │      │ pass_hash │      │ student_id ──────►││
└──────────┘      │ fap_code  │      │ enrolled_at       ││
                  │ full_name │      └───────────────────┘│
                  │ email     │                           │
                  │ role_id ──┘      ┌───────────────────┐│
                  │ created_at│      │    courses        │◄┘
                  │ member_cd │      │───────────────────│
                  └───────────┘      │ course_id         │
                       │             │ subject_code      │
                       │             │ class_name        │
                       │             │ semester           │
                       │             │ lecturer_id ──────►users
                       │             │ created_at         │
                       │             └───────────────────┘
                       │                    │
                       │             ┌──────┴────────────────┐
                       │             │ attendance_sessions    │
                       │             │───────────────────────│
                       │             │ session_id             │
                       │             │ course_id  ──────────►courses
                       │             │ slot_number (1-6)      │
                       │             │ qr_code_secret         │
                       │             │ room_network_ip        │
                       │             │ expired_at             │
                       │             │ status (OPEN/CLOSED)   │
                       │             │ created_at             │
                       │             └───────────┬───────────┘
                       │                         │
                       │             ┌───────────┴───────────┐
                       └────────────►│ attendance_records     │
                                     │───────────────────────│
                                     │ record_id              │
                                     │ session_id ──────────►sessions
                                     │ student_id ──────────►users
                                     │ status (PRESENT/...)   │
                                     │ checked_at             │
                                     └───────────────────────┘

┌─────────────────────────┐
│ google_sheet_sync_logs  │
│─────────────────────────│
│ log_id                  │
│ action                  │
│ status                  │
│ details                 │
│ synced_at               │
└─────────────────────────┘
```

### 5.2. Chi tiết các bảng

| Bảng                     | Mô tả                                         |
|--------------------------|------------------------------------------------|
| `roles`                  | Phân quyền: STUDENT, LECTURER, ADMIN           |
| `users`                  | Thông tin người dùng (SV, GV, Admin)           |
| `courses`                | Khóa học / Lớp học phần                        |
| `enrollments`            | Đăng ký SV vào khóa học                        |
| `attendance_sessions`    | Buổi điểm danh (chứa QR secret, thời hạn)     |
| `attendance_records`     | Bản ghi điểm danh của từng SV                  |
| `google_sheet_sync_logs` | Nhật ký đồng bộ dữ liệu với Google Sheets      |

---

## 6. Hệ thống bảo mật (Security)

### 6.1. Xác thực JWT (JSON Web Token)

- Khi đăng nhập thành công, server trả về **JWT Token** (có hiệu lực 24 giờ).
- Mỗi request tiếp theo phải gửi kèm header: `Authorization: Bearer <token>`.
- JWT chứa các thông tin: `username`, `user_id`, `role`, `fap_code`.

### 6.2. Phân quyền (Roles)

| Vai trò      | Quyền hạn                                                             |
|-------------|------------------------------------------------------------------------|
| `STUDENT`   | Đăng ký khóa học, điểm danh qua QR, xem khóa học đã đăng ký          |
| `LECTURER`  | Tạo/sửa/xóa khóa học, tạo/đóng buổi điểm danh, xem lịch sử, xuất CSV|
| `ADMIN`     | Toàn quyền (tất cả quyền của STUDENT + LECTURER)                      |

### 6.3. Endpoints công khai (Không cần Token)

| Endpoint                  | Mô tả                          |
|---------------------------|--------------------------------|
| `POST /api/auth/register` | Đăng ký tài khoản              |
| `POST /api/auth/login`    | Đăng nhập lấy JWT Token        |
| `POST /api/users/import-excel` | Import user từ Excel      |
| `/swagger-ui/**`          | Swagger UI API Docs             |
| `/v3/api-docs/**`         | OpenAPI JSON spec               |

---

## 7. Chuẩn Response API

Tất cả API đều trả về cấu trúc `ApiResponse<T>` thống nhất:

### Thành công
```json
{
  "success": true,
  "message": "Thành công",
  "data": { ... },
  "timestamp": "2026-09-23T01:52:16.000Z"
}
```

### Thất bại
```json
{
  "success": false,
  "message": "Mô tả lỗi cụ thể",
  "data": null,
  "timestamp": "2026-09-23T01:52:16.000Z"
}
```

### Lỗi Validation
```json
{
  "success": false,
  "message": "Username không được để trống",
  "data": {
    "username": "Username không được để trống",
    "password": "Mật khẩu phải có ít nhất 6 ký tự"
  },
  "timestamp": "2026-09-23T01:52:16.000Z"
}
```

---

## 8. Cấu hình chạy dự án (Getting Started)

### 8.1. Yêu cầu hệ thống

- **Java** 21+
- **MySQL** 8.x (hoặc MariaDB)
- **Maven** 3.9+ (hoặc dùng Maven Wrapper có sẵn)
- **Google Cloud** credentials.json (cho tính năng Google Sheets)

### 8.2. Cấu hình Database

Sửa file `src/main/resources/application.properties` hoặc đặt biến môi trường:

| Biến môi trường          | Giá trị mặc định     | Mô tả                        |
|--------------------------|----------------------|-------------------------------|
| `DB_HOST`                | `localhost`          | Host MySQL                    |
| `DB_PORT`                | `3306`               | Port MySQL                    |
| `DB_NAME`                | `fap_attendance_db`  | Tên database                  |
| `DB_USERNAME`            | `root`               | Username MySQL                |
| `DB_PASSWORD`            | `12345`              | Password MySQL                |
| `GOOGLE_SPREADSHEET_ID`  | *(bắt buộc)*        | ID Google Spreadsheet         |

### 8.3. Chạy dự án

```bash
# Clone repository
git clone <repo-url>
cd attendance_backend

# Chạy với Maven Wrapper (không cần cài Maven)
./mvnw spring-boot:run         # Linux/Mac
mvnw.cmd spring-boot:run       # Windows

# Hoặc build JAR rồi chạy
./mvnw clean package -DskipTests
java -jar target/attendance_backend-0.0.1-SNAPSHOT.jar
```

### 8.4. Truy cập Swagger UI

Sau khi khởi động, truy cập:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **API Docs JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 9. Dual-Database: MySQL + Google Sheets

Dự án sử dụng **kiến trúc dual-database**:

| Thành phần        | MySQL (JPA)                      | Google Sheets API                  |
|-------------------|----------------------------------|------------------------------------|
| **Vai trò**       | Database chính (CRUD nhanh)      | Database phụ (đồng bộ, backup)     |
| **Truy vấn**      | Spring Data JPA Repository       | GoogleSheetsService                |
| **Cấu hình**      | `application.properties`        | `credentials.json` + Spreadsheet ID|

### Các Sheet trên Google Sheets

Dữ liệu được tổ chức thành 7 tab (sheet), mỗi tab tương ứng 1 bảng:

| Sheet Name                 | Tương ứng bảng MySQL          |
|----------------------------|-------------------------------|
| `ROLES`                    | `roles`                       |
| `USERS`                    | `users`                       |
| `COURSES`                  | `courses`                     |
| `ENROLLMENTS`              | `enrollments`                 |
| `ATTENDANCE_SESSIONS`      | `attendance_sessions`         |
| `ATTENDANCE_RECORDS`       | `attendance_records`          |
| `GOOGLE_SHEET_SYNC_LOGS`   | `google_sheet_sync_logs`      |

---

## 10. Design Patterns & Conventions

| Pattern / Convention          | Áp dụng                                              |
|-------------------------------|-------------------------------------------------------|
| **Layered Architecture**      | Controller → Service → Repository                     |
| **DTO Pattern**               | Request/Response DTOs tách biệt Entity                |
| **Interface-Implementation**  | Service interface + ServiceImpl                       |
| **Builder Pattern**           | Tất cả DTO và Entity dùng `@Builder` (Lombok)        |
| **Global Exception Handling** | `@RestControllerAdvice` xử lý lỗi tập trung          |
| **Stateless JWT**             | Không dùng session, mọi request phải kèm token       |
| **Method-level Security**     | `@PreAuthorize("hasAnyRole(...)")` trên mỗi endpoint |

---

## 11. Quy tắc ID Generation

Tất cả ID đều là `String` (UUID) được sinh tự động trong Service layer:

```java
String userId = UUID.randomUUID().toString();
String courseId = UUID.randomUUID().toString();
// ...
```

---

## 12. Xử lý lỗi (Error Handling)

| Exception                          | HTTP Status | Mô tả                                    |
|------------------------------------|-------------|-------------------------------------------|
| `AppException`                     | 400/4xx     | Lỗi nghiệp vụ tùy chỉnh                 |
| `MethodArgumentNotValidException`  | 400         | Lỗi validation input                     |
| `IllegalArgumentException`         | 400         | Tham số không hợp lệ                     |
| `IllegalStateException`            | 503         | Lỗi trạng thái hệ thống                  |
| `Exception` (catch-all)            | 500         | Lỗi nội bộ không xác định                |
| Chưa đăng nhập / Token hết hạn    | 401         | JwtAuthenticationEntryPoint xử lý         |

---

## 13. Liên hệ

- **Tác giả**: Ngô Hoàng Trường Đạt và các cộng sự
- **Email**: datnhse192964@fpt.edu.vn
- **Môn học**: PRM323 — FPT University
