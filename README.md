# Hệ thống Điểm danh Hỗ trợ FAP (FAP Attendance System)

---

## MỤC LỤC
1. [TÀI LIỆU MÔ TẢ NGỮ CẢNH DỰ ÁN (PROJECT CONTEXT DOCUMENT)](#i-tài-liệu-mô-tả-ngữ-cảnh-dự-án-project-context-document)
   - [Thông tin chung](#thông-tin-chung)
   - [1. Tổng quan dự án](#1-tổng-quan-dự-án)
   - [2. Các phân hệ người dùng & Vai trò (Roles)](#2-các-phân-hệ-người-dùng--vai-trò-roles)
   - [3. Ngôn ngữ & Công nghệ sử dụng (Tech Stack)](#3-ngôn-ngữ--công-nghệ-sử-dụng-tech-stack)
   - [4. Các quy tắc nghiệp vụ và ràng buộc dữ liệu quan trọng](#4-các-quy-tắc-nghiệp-vụ-và-ràng-buộc-dữ-liệu-quan-trọng)
   - [5. Quy hoạch cấu trúc dữ liệu trên Google Sheets](#5-quy-hoạch-cấu-trúc-dữ-liệu-trên-google-sheets)
2. [TÀI LIỆU MÔ TẢ THƯ VIỆN VÀ PHỤ THUỘC BACKEND (LIBRARIES & DEPENDENCIES SPECIFICATION)](#ii-tài-liệu-mô-tả-thư-viện-và-phụ-thuộc-backend-libraries--dependencies-specification)
   - [1. Tổng quan các nhóm thư viện](#1-tổng-quan-các-nhóm-thư-viện)
   - [2. Danh sách chi tiết các Thư viện (pom.xml)](#2-danh-sách-chi-tiết-các-thư-viện-pomxml)
3. [KẾ HOẠCH PHÂN CHIA TÁC VỤ & THỰC THI BACKEND (MASTER BACKEND TASK BREAKDOWN & EXECUTION PLAN)](#iii-kế-hoạch-phân-chia-tác-vụ--thực-thi-backend-master-backend-task-breakdown--execution-plan)
   - [Phần 1: Khởi tạo & Hạ tầng kỹ thuật (Setup & Infrastructure)](#phần-1-giai-đoạn-khởi-tạo--hạ-tầng-kỹ-thuật-setup--infrastructure)
   - [Phần 2: Xác thực & Phân quyền (Identity & RBAC Module)](#phần-2-giai-đoạn-xác-thực--phân-quyền-identity--rbac-module)
   - [Phần 3: Quản lý học phần & Đăng ký (FAP Integration Module)](#phần-3-giai-đoạn-quản-lý-học-phần--đăng-ký-fap-integration-module)
   - [Phần 4: Điểm danh & Mã QR (Attendance Module)](#phần-4-giai-đoạn-điểm-danh--mã-qr-attendance-module)
   - [Phần 5: Đồng bộ, Xuất file & Kiểm thử (Report & Testing)](#phần-5-giai-đoạn-đồng-bộ-xuất-file--kiểm-thử-report--testing)

---

# I. TÀI LIỆU MÔ TẢ NGỮ CẢNH DỰ ÁN (PROJECT CONTEXT DOCUMENT)

### Thông tin chung
- **Tên dự án:** Hệ thống Điểm danh Hỗ trợ FAP (FAP Attendance System)
- **Tác giả / Nhóm phát triển:** Ngô Hoàng Trường Đạt và các cộng sự (Đại học FPT - Chuyên ngành Kỹ thuật Phần mềm)
- **Mục tiêu:** Xây dựng giải pháp điểm danh linh hoạt, kết nối với hệ thống FAP, sử dụng Google Sheets làm cơ sở dữ liệu trung gian và mã QR giới hạn thời gian theo mạng trường học.

---

### 1. Tổng quan dự án
Hệ thống được thiết kế để giải quyết bài toán điểm danh lớp học cho sinh viên và giảng viên tại trường đại học. Thay vì sử dụng các cơ sở dữ liệu cồng kềnh, dự án tận dụng **Google Sheets API** làm cơ sở dữ liệu dạng bảng tính giúp dễ dàng quản lý, kiểm tra trực quan và trích xuất dữ liệu ra file `.csv` nhanh chóng.

Ứng dụng hỗ trợ đa nền tảng với giao diện người dùng bằng **Flutter / ReactJS** cho phép sinh viên truy cập qua website/mobile để điểm danh, kết hợp với Backend mạnh mẽ bằng **Java Spring Boot**.

---

### 2. Các phân hệ người dùng & Vai trò (Roles)

#### 👨‍🏫 LECTURER (Giảng viên)
- Đăng nhập và quản lý tài khoản cá nhân.
- Thực hiện Thêm, Sửa, Xóa, Xem danh sách khóa học (**CRUD Courses**).
- Tạo buổi học (**Attendance Sessions**) cho từng tiết học (Slot 1-6), hệ thống tự sinh mã QR bảo mật có thời hạn (`expired_at`) và giới hạn địa chỉ IP mạng trường (`room_network_ip`).
- Trích xuất dữ liệu điểm danh ra file `.csv` hoặc đồng bộ dữ liệu.

#### 👨‍🎓 STUDENT (Sinh viên)
- Đăng nhập và quản lý tài khoản với mã định danh trường học bắt buộc (`fap_code`).
- Đăng ký tham gia các khóa học (**Enrollments**).
- Quét mã QR do giảng viên cung cấp trong khoảng thời gian hiệu lực để thực hiện điểm danh.

---

### 3. Ngôn ngữ & Công nghệ sử dụng (Tech Stack)
- **Frontend:** Flutter (Mobile/Desktop) hoặc ReactJS (Web).
- **Backend:** Java 21, Spring Boot 3.x.
- **Bảo mật:** Spring Security, JSON Web Token (JWT), mã hóa mật khẩu BCrypt.
- **Tiện ích xử lý:** Google ZXing (Tạo và đọc mã QR).
- **Cơ sở dữ liệu:** Google Sheets API v4 (Thay thế Database quan hệ truyền thống).

---

### 4. Các quy tắc nghiệp vụ và ràng buộc dữ liệu quan trọng

#### Định dạng mã sinh viên (`fap_code`)
- Bắt buộc tuân theo biểu thức chính quy:
  ```regex
  ^[A-Z]{2}\d{6}$
  ```
  *(2 chữ cái in hoa đầu và 6 chữ số ở sau, ví dụ: `SE192964`)*

#### Ràng buộc điểm danh qua mã QR
- **Thời hạn hiệu lực:** Mã QR chỉ có hiệu lực trong khoảng thời gian giới hạn (`expired_at`).
- **Địa chỉ mạng:** Kiểm tra thiết bị và địa chỉ IP mạng trường học (`room_network_ip`) theo cấu hình buổi học.
- **Tính duy nhất:** Ngăn chặn việc điểm danh trùng lặp (sinh viên chỉ được điểm danh 1 lần duy nhất cho mỗi `session_id`).

---

### 5. Quy hoạch cấu trúc dữ liệu trên Google Sheets
Hệ thống ánh xạ các thực thể thành các Sheet (Tab) độc lập:

| Tên Sheet | Mô tả chức năng |
| :--- | :--- |
| **`ROLES`** | Quản lý phân quyền (`ADMIN`, `LECTURER`, `STUDENT`). |
| **`USERS`** | Lưu thông tin tài khoản, mật khẩu băm, và `fap_code` của người dùng. |
| **`COURSES`** | Lưu thông tin học phần, lớp học phần do giảng viên phụ trách. |
| **`ENROLLMENTS`** | Lưu danh sách sinh viên đăng ký tham gia các khóa học. |
| **`ATTENDANCE_SESSIONS`** | Quản lý phiên điểm danh, slot học, mã bảo mật QR và thời gian hết hạn. |
| **`ATTENDANCE_RECORDS`** | Lưu lịch sử điểm danh (`PRESENT`, `ABSENT`) của sinh viên theo từng buổi. |
| **`GOOGLE_SHEET_SYNC_LOGS`** | Lưu nhật ký trạng thái đồng bộ dữ liệu và xuất báo cáo. |

---

# II. TÀI LIỆU MÔ TẢ THƯ VIỆN VÀ PHỤ THUỘC BACKEND (LIBRARIES & DEPENDENCIES SPECIFICATION)

### Thông tin nền tảng
- **Dự án:** FAP Attendance System
- **Công nghệ nền tảng:** Java 21, Spring Boot 3.x, Maven

---

### 1. Tổng quan các nhóm thư viện
Hệ thống Backend sử dụng các thư viện và gói phụ thuộc (dependencies) được phân chia theo từng nhóm chức năng cụ thể như sau:

---

### 2. Danh sách chi tiết các Thư viện (`pom.xml`)

#### A. Nhóm Core & Web (Xây dựng nền tảng ứng dụng và REST APIs)
- **`spring-boot-starter-web`**
  - *Mục đích:* Thư viện cốt lõi để xây dựng các RESTful APIs, cung cấp `DispatcherServlet` và nhúng sẵn Tomcat Server để giao tiếp HTTP/HTTPS với Frontend (Flutter / ReactJS).
- **`spring-boot-starter-validation`**
  - *Mục đích:* Hỗ trợ kiểm tra, ràng buộc dữ liệu đầu vào (Bean Validation) thông qua các annotation (ví dụ: kiểm tra định dạng mã sinh viên `fap_code` theo biểu thức chính quy `^[A-Z]{2}\d{6}$, không để trống thông tin).
- **`spring-boot-starter-devtools`**
  - *Mục đích:* Hỗ trợ tính năng tự động khởi động lại ứng dụng (LiveReload/Restart) trong quá trình lập trình, giúp tăng tốc độ phát triển.

#### B. Nhóm Bảo mật & Xác thực (Security & Authentication)
- **`spring-boot-starter-security`**
  - *Mục đích:* Cung cấp khung bảo mật toàn diện cho ứng dụng, cấu hình cơ chế mã hóa mật khẩu (`BCryptPasswordEncoder`) và kiểm soát phân quyền truy cập (`ROLE_ADMIN`, `ROLE_LECTURER`, `ROLE_STUDENT`).
- **`jjwt-api`, `jjwt-impl`, `jjwt-jackson` (JSON Web Token - v0.11.5)**
  - *Mục đích:* Bộ thư viện dùng để tạo (generate), ký số và xác thực JWT Token. Giúp duy trì phiên đăng nhập cho người dùng mà không cần lưu trữ trạng thái trên Server.

#### C. Nhóm Cơ sở dữ liệu & Tích hợp Google (Database & Cloud Integration)
- **`google-api-services-sheets` (v4-rev20220927-2.0.0)**
  - *Mục đích:* Thư viện chính thức từ Google, cung cấp các phương thức để Backend kết nối, đọc, ghi, cập nhật dữ liệu trực tiếp lên các Google Sheets (đóng vai trò thay thế Database quan hệ truyền thống).
- **`google-auth-library-oauth2-http` (v1.19.0)**
  - *Mục đích:* Hỗ trợ cơ chế xác thực an toàn thông qua Service Account (`credentials.json`) để cấp quyền cho ứng dụng Java truy cập vào Google Sheets và Google Drive API.

#### D. Nhóm Tiện ích & Tài liệu API (Utilities & Documentation)
- **`core` & `javase` (Google ZXing - v3.5.3)**
  - *Mục đích:* Thư viện xử lý mã vạch và mã QR của Google. Được sử dụng ở phía Backend để sinh ra các chuỗi mã QR bảo mật cho các buổi điểm danh (Attendance Sessions).
- **`springdoc-openapi-starter-webmvc-ui` (v2.5.0)**
  - *Mục đích:* Tự động tạo tài liệu API và cung cấp giao diện Swagger UI trực quan (`/swagger-ui/index.html`). Giúp nhóm Frontend dễ dàng tra cứu endpoint, xem cấu trúc dữ liệu và thử nghiệm gọi API trực tiếp.
- **`lombok`**
  - *Mục đích:* Thư viện giúp giảm thiểu mã nguồn lặp (boilerplate code) bằng cách tự động sinh ra các hàm getter, setter, constructor, toString() thông qua các annotation (`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`).

#### E. Nhóm Kiểm thử (Testing)
- **`spring-boot-starter-test`**
  - *Mục đích:* Cung cấp các công cụ tiêu chuẩn để viết Unit Test và Integration Test (JUnit 5, Mockito, Spring Test) nhằm kiểm tra độ ổn định của hệ thống Backend.

---

# III. KẾ HOẠCH PHÂN CHIA TÁC VỤ & THỰC THI BACKEND (MASTER BACKEND TASK BREAKDOWN & EXECUTION PLAN)

### Thông tin kế hoạch
- **Dự án:** FAP Attendance System (Java 21 + Spring Boot + Google Sheets DB)
- **Mục tiêu:** Kế hoạch phân chia tác vụ chi tiết, tuần tự và liên kết chặt chẽ cho đội ngũ lập trình Backend, giúp dễ dàng phân công công việc đến từng thành viên hoặc AI agent mà không sợ bị rời rạc.

---

### PHẦN 1: GIAI ĐOẠN KHỞI TẠO & HẠ TẦNG KỸ THUẬT (SETUP & INFRASTRUCTURE)

#### 📌 [TASK-01] Khởi tạo Project & Cấu hình Môi trường
- **1.1.** Tạo project Spring Boot mới trên Spring Initializr với Java 21, Maven, cấu hình Artifact tên là `attendance_backend`.
- **1.2.** Thêm toàn bộ các dependency vào file `pom.xml`: Spring Web, Validation, Security, JJwt, ZXing, SpringDoc OpenAPI, Google Sheets API Client, Google Auth Library, và Lombok.
- **1.3.** Cấu hình file `application.yml` hoặc `application.properties` (cấu hình port, tên ứng dụng, cấu hình bảo mật JWT secret key, thông tin cấu hình Google Cloud Service Account).
- **1.4.** Đưa file `credentials.json` của Google Cloud Service Account vào thư mục bảo mật (`src/main/resources`) và cấu hình `.gitignore` để bảo mật thông tin xác thực.

#### 📌 [TASK-02] Xây dựng Google Sheets Service (Database Layer)
- **2.1.** Viết lớp `GoogleSheetsConfig` để khởi tạo Bean kết nối `com.google.api.services.sheets.v4.Sheets` sử dụng Service Account credentials.
- **2.2.** Viết lớp tiện ích `GoogleSheetsService` hỗ trợ các thao tác cơ bản:
  - Đọc dữ liệu từ một dải ô cụ thể (`readRange(spreadsheetId, range)`).
  - Ghi thêm dòng dữ liệu mới vào cuối sheet (`appendRow(spreadsheetId, range, listValues)`).
  - Cập nhật dữ liệu hoặc tìm kiếm dòng theo điều kiện khóa chính (`updateRow`, `findRowIndex`).
- **2.3.** Định nghĩa cấu trúc Sheet ID và tên các Tab tương ứng với ERD: `ROLES`, `USERS`, `COURSES`, `ENROLLMENTS`, `ATTENDANCE_SESSIONS`, `ATTENDANCE_RECORDS`, `GOOGLE_SHEET_SYNC_LOGS`.

---

### PHẦN 2: GIAI ĐOẠN XÁC THỰC & PHÂN QUYỀN (IDENTITY & RBAC MODULE)

#### 📌 [TASK-03] Xây dựng Quản lý Người dùng & Đăng ký (Users & Register)
- **3.1.** Thiết kế tầng Model/DTO cho yêu cầu đăng ký (`RegisterRequest`) và đăng nhập (`LoginRequest`).
- **3.2.** Thêm ràng buộc Bean Validation vào `RegisterRequest`:
  - Bắt buộc có `fap_code` và áp dụng biểu thức chính quy `^[A-Z]{2}\d{6}$` (Ví dụ: `SE192964`) đối với tài khoản là `STUDENT`.
  - Validate các trường `username`, `password`, `email`, `fullName`, `roleId`.
- **3.3.** Xây dựng `AuthService.register()`: Kiểm tra trùng lặp username hoặc `fap_code` trên sheet `USERS`, mã hóa mật khẩu bằng `BCryptPasswordEncoder`, sau đó ghi dòng dữ liệu mới vào sheet `USERS`.

#### 📌 [TASK-04] Xây dựng Đăng nhập & Phân quyền Bảo mật (Authentication & JWT)
- **4.1.** Xây dựng `AuthService.login()`: Đọc sheet `USERS`, so khớp username và kiểm tra mật khẩu đã băm.
- **4.2.** Cấu hình thư viện JJwt để sinh mã JWT Token chứa các thông tin định danh (`user_id`, `username`, `role`) sau khi đăng nhập thành công.
- **4.3.** Viết lớp `JwtAuthenticationFilter` để chặn các HTTP request, trích xuất token từ header `Authorization`, xác thực tính hợp lệ và thiết lập Security Context.
- **4.4.** Cấu hình `SecurityConfig` (Spring Security 6.x `SecurityFilterChain`): Phân quyền truy cập các endpoint theo role (`ADMIN`, `LECTURER`, `STUDENT`) và cấu hình cho phép public các API đăng nhập/đăng ký/Swagger UI.

---

### PHẦN 3: GIAI ĐOẠN QUẢN LÝ HỌC PHẦN & ĐĂNG KÝ (FAP INTEGRATION MODULE)

#### 📌 [TASK-05] Xây dựng CRUD Khóa học (Courses Management - Cho LECTURER)
- **5.1.** Tạo DTO cho Course (`CourseRequest`, `CourseResponse`).
- **5.2.** Viết API `GET /api/courses`: Lấy toàn bộ danh sách khóa học hoặc lọc theo giảng viên (`lecturer_id`) từ sheet `COURSES`.
- **5.3.** Viết API `POST /api/courses`: Cho phép giảng viên tạo khóa học mới (`subject_code`, `class_name`, `semester`, `lecturer_id`). Kiểm tra quyền hạn `LECTURER` trước khi ghi vào sheet `COURSES`.
- **5.4.** Viết API `PUT /api/courses/{id}` và `DELETE /api/courses/{id}` để cập nhật và xóa thông tin khóa học trên Google Sheets.

#### 📌 [TASK-06] Xây dựng Quản lý Đăng ký Khóa học (Enrollments - Cho STUDENT)
- **6.1.** Tạo DTO cho Enrollment (`EnrollmentRequest`).
- **6.2.** Viết API `POST /api/enrollments`: Cho phép sinh viên đăng ký tham gia một lớp học phần (`course_id`).
- **6.3.** Viết logic kiểm tra: Đảm bảo sinh viên tồn tại, khóa học tồn tại, và sinh viên chưa từng đăng ký khóa học đó trước đó (tránh trùng lặp), sau đó ghi dữ liệu vào sheet `ENROLLMENTS` kèm thời gian `enrolled_at`.

---

### PHẦN 4: GIAI ĐOẠN ĐIỂM DANH & MÃ QR (ATTENDANCE MODULE)

#### 📌 [TASK-07] Xây dựng Quản lý Buổi học & Tạo mã QR (Attendance Sessions)
- **7.1.** Tạo DTO cho Session (`SessionCreateRequest`).
- **7.2.** Viết API `POST /api/sessions`: Giảng viên tạo buổi điểm danh cho một khóa học (`course_id`), chỉ định `slot_number` (từ 1 đến 6), thời gian hết hạn `expired_at`, và địa chỉ IP phòng học `room_network_ip`.
- **7.3.** Tích hợp thư viện Google ZXing để tạo mã QR dạng chuỗi bảo mật (`qr_code_secret`) đại diện cho phiên học đó.
- **7.4.** Lưu thông tin buổi học vào sheet `ATTENDANCE_SESSIONS` với trạng thái mặc định là `ACTIVE`.

#### 📌 [TASK-08] Xây dựng Logic Điểm danh cho Sinh viên (Check-in via QR)
- **8.1.** Tạo DTO nhận thông tin check-in (`CheckInRequest` chứa `session_id` và mã QR/token).
- **8.2.** Viết API `POST /api/attendance/check-in` dành cho sinh viên.
- **8.3.** Triển khai các lớp kiểm tra nghiệp vụ (**Validation Chain**) chặt chẽ tại Backend:
  - **Kiểm tra 1:** Buổi học (`session_id`) có tồn tại và trạng thái có phải là `ACTIVE` không?
  - **Kiểm tra 2:** Thời gian hiện tại đã vượt quá mốc `expired_at` chưa? (Nếu quá hạn $\rightarrow$ Từ chối điểm danh).
  - **Kiểm tra 3:** Sinh viên có nằm trong danh sách đã đăng ký khóa học tương ứng tại sheet `ENROLLMENTS` hay không?
  - **Kiểm tra 4:** Sinh viên đã thực hiện điểm danh buổi này trước đó chưa (tránh điểm danh lặp lại)?
  - **Kiểm tra 5:** Ràng buộc kiểm tra địa chỉ IP mạng trường (`room_network_ip`) nếu hệ thống yêu cầu.
- **8.4.** Nếu vượt qua mọi điều kiện, ghi nhận bản ghi mới vào sheet `ATTENDANCE_RECORDS` với trạng thái `PRESENT` và thời gian `checked_at`.

---

### PHẦN 5: GIAI ĐOẠN ĐỒNG BỘ, XUẤT FILE & KIỂM THỬ (REPORT & TESTING)

#### 📌 [TASK-09] Xây dựng Tính năng Export file .CSV và Sync Logs
- **9.1.** Viết API `GET /api/attendance/export/{sessionId}`: Cho phép trích xuất toàn bộ danh sách điểm danh (`ATTENDANCE_RECORDS`) của một buổi học hoặc lớp học ra định dạng tệp `.csv`.
- **9.2.** Xây dựng cơ chế ghi nhật ký vào sheet `GOOGLE_SHEET_SYNC_LOGS` để lưu lại trạng thái đồng bộ (`SUCCESS` hoặc `FAILED`) kèm thời gian `synced_at`.

#### 📌 [TASK-10] Tích hợp Swagger OpenAPI & Kiểm thử tổng thể
- **10.1.** Cấu hình SpringDoc OpenAPI để tự động sinh tài liệu Swagger UI, thêm cấu hình bảo mật Bearer Token (JWT) trực tiếp trên giao diện tài liệu API.
- **10.2.** Rà soát toàn bộ các Controller, Service, viết Unit Test cơ bản (JUnit 5) cho các logic nghiệp vụ quan trọng (đặc biệt là logic check-in và regex `fap_code`).
- **10.3.** Tiến hành chạy thử nghiệm toàn bộ hệ thống (Integration Test) để đảm bảo thông tin truyền nhận mượt mà giữa Backend và Google Sheets.
