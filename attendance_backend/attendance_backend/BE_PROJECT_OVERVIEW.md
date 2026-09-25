# 📚 FAP Student Attendance System — Backend

> Hệ thống điểm danh sinh viên FPT dựa trên QR Code, xây dựng bằng **Spring Boot 4.1** + **MySQL** + **Google Sheets** (dual-database).

---

## 1. Tổng quan dự án

**FAP Student Attendance System** là hệ thống backend RESTful API phục vụ việc điểm danh sinh viên tại trường Đại học FPT. Hệ thống hỗ trợ:

- **Giảng viên**: Tạo buổi học, sinh mã QR điểm danh kèm mã PIN 6 chữ số (`sessionCode`), kiểm tra kết nối mạng Wi-Fi phòng học (`roomNetworkIp`), import danh sách sinh viên vào khóa học bằng Excel (.xlsx), quản lý khóa học (thiết lập mật khẩu bảo vệ lớp học), theo dõi sinh viên, xuất file báo cáo điểm danh Excel (.xlsx) 100% danh sách lớp (PRESENT/ABSENT).
- **Sinh viên**: Tìm kiếm khóa học linh hoạt (theo tên lớp, môn học, học kỳ, giảng viên), đăng ký khóa học (với mật khẩu nếu có), xem danh sách các khóa học mình tham gia kèm tên giảng viên, điểm danh linh hoạt qua quét mã QR hoặc nhập mã PIN 6 chữ số, xem lịch sử điểm danh cá nhân của mình.
- **Người dùng (tất cả vai trò)**: Cập nhật thông tin cá nhân (họ tên, email) và đổi mật khẩu an toàn, hệ thống tự động đồng bộ sang Google Sheets.
- **Admin**: Quản lý toàn bộ hệ thống, import dữ liệu sinh viên/người dùng từ file Excel.

### Quy trình nghiệp vụ chính

```
Giảng viên tạo khóa học (hỗ trợ đặt mật khẩu) hoặc import danh sách sinh viên bằng Excel
    → Sinh viên tìm kiếm & đăng ký khóa học
        → Sinh viên xem danh sách khóa học tham gia (kèm tên giảng viên & bộ lọc)
            → Giảng viên tạo buổi điểm danh (sinh QR Code & mã PIN 6 số, tùy chọn Wi-Fi IP)
                → Sinh viên quét QR hoặc nhập PIN để điểm danh (hệ thống kiểm tra IP/Wi-Fi nếu có)
                    → Sinh viên xem lịch sử điểm danh cá nhân
                        → Giảng viên xuất báo cáo Excel (.xlsx đầy đủ 100% sinh viên) / CSV / Đồng bộ Google Sheets
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
        │   │   ├── CourseController.java           # Quản lý, tìm kiếm, import SV & đăng ký khóa học (hỗ trợ /my-courses cho GV & SV)
        │   │   ├── LecturerController.java         # Thống kê Dashboard & tổng quan dành riêng cho Giảng viên
        │   │   ├── EnrollmentController.java       # Đăng ký khóa học & danh sách lớp
        │   │   ├── AttendanceSessionController.java # Quản lý buổi điểm danh (QR & PIN code)
        │   │   ├── AttendanceController.java       # Điểm danh, lịch sử cá nhân, xuất CSV & Excel
        │   │   ├── UserController.java             # Profile, đổi mật khẩu & import Excel
        │   │   └── SyncLogController.java          # Nhật ký đồng bộ
        │   │
        │   ├── dto/                                # 📦 Data Transfer Objects
        │   │   ├── request/                        # Input DTOs (Client → Server)
        │   │   │   ├── LoginRequest.java
        │   │   │   ├── RegisterRequest.java
        │   │   │   ├── CourseRequest.java          # Tạo/cập nhật khóa học (hỗ trợ mật khẩu)
        │   │   │   ├── CourseRegisterRequest.java   # Đăng ký khóa học (courseId, userId, coursePassword)
        │   │   │   ├── EnrollmentRequest.java
        │   │   │   ├── SessionCreateRequest.java
        │   │   │   ├── CheckInRequest.java         # Điểm danh qua QR hoặc mã PIN 6 số
        │   │   │   ├── UpdateProfileRequest.java   # Cập nhật thông tin cá nhân
        │   │   │   └── ChangePasswordRequest.java  # Đổi mật khẩu
        │   │   └── response/                       # Output DTOs (Server → Client)
        │   │       ├── ApiResponse.java            # Cấu trúc response chuẩn
        │   │       ├── AuthResponse.java
        │   │       ├── UserResponse.java
        │   │       ├── CourseResponse.java
        │   │       ├── LecturerDashboardStatsResponse.java # Thống kê tổng quan Dashboard giảng viên
        │   │       ├── RecentCourseResponse.java   # Thông tin tóm tắt khóa học gần đây
        │   │       ├── EnrollmentResponse.java
        │   │       ├── SessionResponse.java        # Thông tin buổi điểm danh (kèm sessionCode)
        │   │       ├── AttendanceRecordResponse.java
        │   │       ├── AttendanceHistoryResponse.java # Lịch sử điểm danh cá nhân
        │   │       ├── ImportStudentsResponse.java    # Kết quả import sinh viên vào khóa học
        │   │       └── SyncLogResponse.java
        │   │
        │   ├── model/                              # 🗄️ JPA Entities (Database Tables)
        │   │   ├── User.java                       # Bảng users
        │   │   ├── Role.java                       # Bảng roles
        │   │   ├── Course.java                     # Bảng courses
        │   │   ├── Enrollment.java                 # Bảng enrollments
        │   │   ├── AttendanceSession.java          # Bảng attendance_sessions (session_code)
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
        │   │   ├── CourseService.java              # Interface khóa học & import SV
        │   │   ├── LecturerService.java            # Interface thống kê Dashboard giảng viên
        │   │   ├── EnrollmentService.java          # Interface đăng ký
        │   │   ├── AttendanceSessionService.java   # Interface buổi điểm danh
        │   │   ├── AttendanceRecordService.java    # Interface điểm danh & xuất Excel
        │   │   ├── UserService.java                # Interface thông tin cá nhân & mật khẩu
        │   │   ├── ExcelImportService.java         # Interface import Excel
        │   │   ├── GoogleSheetsService.java        # Interface Google Sheets
        │   │   ├── SyncLogService.java             # Interface nhật ký
        │   │   └── impl/                           # Implementations
        │   │       ├── AuthServiceImpl.java
        │   │       ├── CourseServiceImpl.java
        │   │       ├── LecturerServiceImpl.java
        │   │       ├── EnrollmentServiceImpl.java
        │   │       ├── AttendanceSessionServiceImpl.java
        │   │       ├── AttendanceRecordServiceImpl.java
        │   │       ├── UserServiceImpl.java
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
        │       ├── TotpUtil.java                   # Thuật toán TOTP sinh/xác thực Dynamic QR Token 10s (Stateless)
        │       ├── IpUtil.java                     # Trích xuất IP Proxy/Cloudflare & Subnet /24 Matching
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
                       │             │ password           │
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

| Bảng                     | Mô tả                                                             |
|--------------------------|-------------------------------------------------------------------|
| `roles`                  | Phân quyền: STUDENT, LECTURER, ADMIN                              |
| `users`                  | Thông tin người dùng (SV, GV, Admin, hỗ trợ đổi pass, cập nhật)   |
| `courses`                | Khóa học / Lớp học phần (có mật khẩu bảo vệ)                      |
| `enrollments`            | Đăng ký SV vào khóa học (thủ công hoặc import Excel)              |
| `attendance_sessions`    | Buổi điểm danh (chứa QR secret, sessionCode 6 số, roomNetworkIp, hostPublicIp, roomBssid) |
| `attendance_records`     | Bản ghi điểm danh của từng SV (PRESENT/ABSENT, lưu deviceId chống gian lận máy)            |
| `google_sheet_sync_logs` | Nhật ký đồng bộ dữ liệu với Google Sheets                                                 |

---

## 6. Hệ thống bảo mật (Security)

### 6.1. Xác thực JWT (JSON Web Token)

- Khi đăng nhập thành công, server trả về **JWT Token** (có hiệu lực 24 giờ).
- Mỗi request tiếp theo phải gửi kèm header: `Authorization: Bearer <token>`.
- JWT chứa các thông tin: `username`, `user_id`, `role`, `fap_code`.

### 6.2. Phân quyền (Roles)

| Vai trò      | Quyền hạn                                                                                                                                                                                |
|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `STUDENT`   | Tìm kiếm khóa học nâng cao, đăng ký khóa học (hỗ trợ mật khẩu), xem khóa học tham gia (lọc semester/subjectCode), điểm danh qua QR / mã PIN 6 số (kiểm tra Wi-Fi IP), xem lịch sử cá nhân, đổi mật khẩu, cập nhật profile |
| `LECTURER`  | Tạo/sửa/xóa khóa học (mật khẩu bảo vệ), xem khóa học của tôi (`/api/courses/my-courses`), xem thống kê Dashboard riêng (`/api/lecturer/dashboard-stats`), import sinh viên bằng Excel (.xlsx), tạo buổi điểm danh (QR & PIN 6 số, Wi-Fi IP), đóng buổi điểm danh, xem lịch sử, xuất CSV & Excel 100% sinh viên, đổi mật khẩu, cập nhật profile |
| `ADMIN`     | Toàn quyền (tất cả quyền của STUDENT + LECTURER), import danh sách người dùng hệ thống từ file Excel, xem toàn bộ log đồng bộ Google Sheets                                                |

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

---

## 14. 🚀 Nhật ký nâng cấp: Hệ thống Điểm danh Dynamic QR Code (TOTP) & Wi-Fi Thích Ứng Đa Tầng (Adaptive Wi-Fi Verification)

Nhằm giải quyết triệt để vấn đề gian lận (chụp ảnh màn hình gửi qua Zalo/Messenger, dùng 4G ở nhà điểm danh) và khắc phục các rủi ro mạng thực tế (Cân bằng tải Multi-WAN trường học, lệch IPv4/IPv6, lỗi Android tắt GPS mất BSSID, và môi trường phát triển nội bộ LAN/Localhost), hệ thống đã được nâng cấp toàn diện:

### 14.1. Các thay đổi về Công nghệ & Thuật toán
1. **Thuật toán Dynamic QR TOTP (Stateless & Tùy biến chu kỳ)**:
   - File: `com.example.attendance_backend.util.TotpUtil`
   - Token ngắn hạn (8 ký tự Hex, VD: `4F3A8B12`) sinh theo thuật toán HMAC-SHA256, **chu kỳ làm mới mặc định là 20 giây** (thay vì 10s trước đây để sinh viên có thêm thời gian quét).
   - **Giảng viên có thể tùy biến thời gian này** theo từng buổi học (từ 5 đến 300 giây).
   - Cơ chế xác thực dung sai $\pm 1$ chu kỳ bù trừ độ trễ mạng Internet và tốc độ lấy nét camera.
   - **Tối ưu 100% tài nguyên**: Không cần lưu token vào Database hay Cache, triệt tiêu gánh nặng I/O và CPU khi có hàng trăm lớp cùng điểm danh.
2. **Tiện ích Xử lý IP & Mạng Wi-Fi Thích Ứng Đa Tầng**:
   - File: `com.example.attendance_backend.util.IpUtil`
   - **Bóc tách IP thực**: Ưu tiên đọc qua các proxy header (`CF-Connecting-IP`, `X-Forwarded-For`, `X-Real-IP`, `RemoteAddr`), chuẩn hóa IPv6 localhost về `127.0.0.1`.
   - **Subnet /24 Matching (IPv4)**: So sánh 3 octet đầu của IPv4 giữa Sinh viên và Giảng viên (`118.69.12.*` hoặc `192.168.1.*`), giải quyết triệt để việc Router trường học cân bằng tải Multi-WAN hoặc cấp IP nội bộ qua DHCP.
   - **Prefix /64 Matching (IPv6)**: So sánh 4 hextet đầu của địa chỉ IPv6 khi hai thiết bị kết nối vào mạng router hỗ trợ IPv6.
   - **Tự động nhận diện IP Máy chủ (Local LAN Dev Support)**: Khi Giảng viên tạo buổi học từ Laptop qua `localhost` (`127.0.0.1`), Backend tự động quét các card mạng vật lý đang hoạt động (`NetworkInterface`) để lấy IP LAN Wi-Fi (ví dụ `192.168.1.116`), đồng thời cơ chế so khớp IP tự động liên kết `127.0.0.1` với các interface nội bộ của máy chủ, cho phép Điện thoại và Laptop cùng Wi-Fi điểm danh thành công 100%.
   - **Lọc BSSID ảo & Adaptive Fallback**: Lọc bỏ các mã MAC dummy do Android trả về khi người dùng tắt GPS (`02:00:00:00:00:00`, `unknown`). Nếu điện thoại không lấy được BSSID, Backend tự động chuyển tiếp sang kiểm tra IP mạng để sinh viên không bị gián đoạn.

### 14.2. Các thay đổi về Cơ sở dữ liệu (Database Schema)
- **Bảng `attendance_sessions`**:
  - `qr_refresh_seconds INT DEFAULT 20`: Chu kỳ tự động đổi mã QR TOTP (giây). Mặc định là 20 giây nếu Giảng viên không chỉ định.
  - `host_public_ip VARCHAR(45)`: Tự động chụp lại IP của Giảng viên khi gọi API tạo phiên (hỗ trợ cả Public IP và LAN IP).
  - `room_bssid VARCHAR(50)`: Địa chỉ MAC phần cứng của Router Wi-Fi phòng học lấy từ Mobile App.
- **Bảng `attendance_records`**:
  - `device_id VARCHAR(100)`: Mã định danh phần cứng thiết bị.
  - Quy tắc bảo mật: **Một thiết bị chỉ được điểm danh cho đúng 1 sinh viên trong một buổi học** (chống 1 máy điểm danh hộ nhiều bạn).

### 14.3. Các Endpoint API Mới & Cập nhật
- `POST /api/sessions`: Tự động trích xuất IP của Giảng viên gán vào `hostPublicIp`, hỗ trợ nhận `roomBssid`.
- `GET /api/sessions/{sessionId}/current-token`: Endpoint Polling lấy mã TOTP token mới nhất kèm số giây countdown còn lại.
- `GET /api/sessions/{sessionId}/qr-stream`: Endpoint **Server-Sent Events (SSE)** tự động push token và countdown mỗi 1 giây cho màn hình máy chiếu của Giảng viên.
- `POST /api/attendance/check-in`: Nhận `qrToken`, `bssid`, `deviceId`; Server tự đọc `clientIp` từ socket (bỏ qua `clientIp` trong body để chống IP Spoofing); thực hiện xác thực đa tầng (BSSID $\rightarrow$ IPv4 Subnet /24 $\rightarrow$ IPv6 Prefix /64 $\rightarrow$ LAN Dev).
