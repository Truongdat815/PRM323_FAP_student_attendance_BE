# 🔌 API Endpoints Reference — FAP Attendance System

> Tài liệu API cho lập trình viên Frontend nối endpoint.
> **Base URL**: `http://localhost:8080`
> **Swagger UI**: `http://localhost:8080/swagger-ui.html`

---

## Quy ước chung

### Chuẩn Response

Tất cả API đều trả về dạng `ApiResponse<T>`:

```json
{
  "success": true | false,
  "message": "Mô tả kết quả",
  "data": { ... } | [ ... ] | null,
  "timestamp": "2026-09-23T01:52:16.000Z"
}
```

### Authentication

- Hầu hết API yêu cầu **JWT Bearer Token** trong header:
  ```
  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
  ```
- Các endpoint **KHÔNG cần token**: `/api/auth/register`, `/api/auth/login`, `/api/users/import-excel`

### Phân quyền (Roles)

| Role       | Mô tả                        |
|------------|-------------------------------|
| `STUDENT`  | Sinh viên                     |
| `LECTURER` | Giảng viên                    |
| `ADMIN`    | Quản trị viên (toàn quyền)    |

---

## 1. 🔐 Authentication Controller

**Base path**: `/api/auth`

---

### 1.1. Đăng ký tài khoản

| Thuộc tính   | Giá trị                          |
|-------------|----------------------------------|
| **Tên**     | Register                         |
| **Method**  | `POST`                           |
| **URL**     | `/api/auth/register`             |
| **Auth**    | ❌ Không cần token                |

**Request Body** (`application/json`):

| Field      | Type     | Required | Validation                                             | Mô tả                          |
|------------|----------|----------|--------------------------------------------------------|---------------------------------|
| `username` | `string` | ✅       | 3–50 ký tự                                             | Tên đăng nhập (unique)          |
| `password` | `string` | ✅       | Tối thiểu 6 ký tự                                      | Mật khẩu                       |
| `email`    | `string` | ✅       | Đúng định dạng email                                    | Email                           |
| `fullName` | `string` | ✅       | Không được để trống                                     | Họ và tên                       |
| `roleId`   | `string` | ✅       | `STUDENT` hoặc `LECTURER`                               | Vai trò                         |
| `fapCode`  | `string` | ⚠️       | Regex: `^[A-Z]{2}\d{6}$` — Bắt buộc nếu role=STUDENT  | Mã sinh viên (VD: `SE192964`)   |

**Response** (`201 Created`):

```json
{
  "success": true,
  "message": "Đăng ký tài khoản thành công",
  "data": {
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "username": "datnht",
    "fapCode": "SE192964",
    "fullName": "Ngô Hoàng Trường Đạt",
    "email": "datnhse192964@fpt.edu.vn",
    "roleId": "STUDENT",
    "createdAt": "2026-09-23T08:00:00",
    "memberCode": null
  },
  "timestamp": "2026-09-23T01:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "datnht",
    "password": "123456",
    "email": "datnhse192964@fpt.edu.vn",
    "fullName": "Ngô Hoàng Trường Đạt",
    "roleId": "STUDENT",
    "fapCode": "SE192964"
  }'
```

---

### 1.2. Đăng nhập

| Thuộc tính   | Giá trị                          |
|-------------|----------------------------------|
| **Tên**     | Login                            |
| **Method**  | `POST`                           |
| **URL**     | `/api/auth/login`                |
| **Auth**    | ❌ Không cần token                |

**Request Body** (`application/json`):

| Field      | Type     | Required | Mô tả             |
|------------|----------|----------|--------------------|
| `username` | `string` | ✅       | Tên đăng nhập      |
| `password` | `string` | ✅       | Mật khẩu           |

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkYXRuaHQiLCJ1c2VyX2lkIjoiYTFiMi...",
    "tokenType": "Bearer",
    "user": {
      "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "username": "datnht",
      "fapCode": "SE192964",
      "fullName": "Ngô Hoàng Trường Đạt",
      "email": "datnhse192964@fpt.edu.vn",
      "roleId": "STUDENT",
      "createdAt": "2026-09-23T08:00:00",
      "memberCode": null
    }
  },
  "timestamp": "2026-09-23T01:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "datnht",
    "password": "123456"
  }'
```

> 💡 **Lưu ý**: Lưu lại `data.token` để sử dụng cho các API yêu cầu xác thực.

---

## 2. 📘 Course Controller

**Base path**: `/api/courses`

---

### 2.1. Lấy danh sách khóa học

| Thuộc tính   | Giá trị                                    |
|-------------|---------------------------------------------|
| **Tên**     | Get All Courses                              |
| **Method**  | `GET`                                        |
| **URL**     | `/api/courses`                               |
| **Auth**    | ✅ Bearer Token (Tất cả role)                |

**Query Parameters**:

| Param        | Type     | Required | Mô tả                                           |
|-------------|----------|----------|--------------------------------------------------|
| `lecturerId`| `string` | ❌       | Lọc theo ID giảng viên (nếu không truyền = lấy hết) |

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Lấy danh sách khóa học thành công",
  "data": [
    {
      "courseId": "c1d2e3f4-a5b6-7890-cdef-1234567890ab",
      "subjectCode": "PRM392",
      "className": "SE1701",
      "semester": "FA24",
      "lecturerId": "l1m2n3o4-p5q6-7890-rstu-vwxyz1234567",
      "lecturerName": "Nguyễn Văn A",
      "createdAt": "2026-09-20T10:00:00"
    }
  ],
  "timestamp": "2026-09-23T01:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
# Lấy tất cả
curl -X GET http://localhost:8080/api/courses \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."

# Lọc theo giảng viên
curl -X GET "http://localhost:8080/api/courses?lecturerId=l1m2n3o4-p5q6-7890-rstu-vwxyz1234567" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### 2.2. Xem chi tiết khóa học

| Thuộc tính   | Giá trị                              |
|-------------|---------------------------------------|
| **Tên**     | Get Course By ID                      |
| **Method**  | `GET`                                 |
| **URL**     | `/api/courses/{id}`                   |
| **Auth**    | ✅ Bearer Token (Tất cả role)         |

**Path Parameters**:

| Param | Type     | Mô tả          |
|-------|----------|-----------------|
| `id`  | `string` | ID của khóa học |

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Lấy thông tin khóa học thành công",
  "data": {
    "courseId": "c1d2e3f4-a5b6-7890-cdef-1234567890ab",
    "subjectCode": "PRM392",
    "className": "SE1701",
    "semester": "FA24",
    "lecturerId": "l1m2n3o4-p5q6-7890-rstu-vwxyz1234567",
    "lecturerName": "Nguyễn Văn A",
    "createdAt": "2026-09-20T10:00:00"
  },
  "timestamp": "2026-09-23T01:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X GET http://localhost:8080/api/courses/c1d2e3f4-a5b6-7890-cdef-1234567890ab \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### 2.3. Tạo khóa học mới

| Thuộc tính   | Giá trị                              |
|-------------|---------------------------------------|
| **Tên**     | Create Course                         |
| **Method**  | `POST`                                |
| **URL**     | `/api/courses`                        |
| **Auth**    | ✅ Bearer Token — `LECTURER`, `ADMIN` |

**Request Body** (`application/json`):

| Field         | Type     | Required | Mô tả                                              |
|--------------|----------|----------|-----------------------------------------------------|
| `subjectCode`| `string` | ✅       | Mã môn học (VD: `PRM392`)                           |
| `className`  | `string` | ✅       | Tên lớp học phần (VD: `SE1701`)                     |
| `semester`   | `string` | ✅       | Học kỳ (VD: `FA24`, `SP25`)                         |
| `password`   | `string` | ❌       | Mật khẩu khóa học (tùy chọn, được băm bảo mật BCrypt)|
| `lecturerId` | `string` | ❌       | ID giảng viên (nếu bỏ trống = lấy từ user đang login)|

**Response** (`201 Created`):

```json
{
  "success": true,
  "message": "Tạo khóa học thành công",
  "data": {
    "courseId": "new-uuid-generated",
    "subjectCode": "PRM392",
    "className": "SE1701",
    "semester": "FA24",
    "lecturerId": "l1m2n3o4-p5q6-7890-rstu-vwxyz1234567",
    "lecturerName": null,
    "createdAt": "2026-09-23T08:30:00"
  },
  "timestamp": "2026-09-23T01:30:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "subjectCode": "PRM392",
    "className": "SE1701",
    "semester": "FA24",
    "password": "secretPassword123"
  }'
```

---

### 2.4. Cập nhật khóa học

| Thuộc tính   | Giá trị                              |
|-------------|---------------------------------------|
| **Tên**     | Update Course                         |
| **Method**  | `PUT`                                 |
| **URL**     | `/api/courses/{id}`                   |
| **Auth**    | ✅ Bearer Token — `LECTURER`, `ADMIN` |

**Path Parameters**:

| Param | Type     | Mô tả          |
|-------|----------|-----------------|
| `id`  | `string` | ID của khóa học |

**Request Body** (`application/json`):

| Field         | Type     | Required | Mô tả                                               |
|--------------|----------|----------|------------------------------------------------------|
| `subjectCode`| `string` | ✅       | Mã môn học                                           |
| `className`  | `string` | ✅       | Tên lớp học phần                                      |
| `semester`   | `string` | ✅       | Học kỳ                                               |
| `password`   | `string` | ❌       | Mật khẩu khóa học mới (nếu muốn thay đổi mật khẩu)   |
| `lecturerId` | `string` | ❌       | ID giảng viên                                         |

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Cập nhật thông tin khóa học thành công",
  "data": {
    "courseId": "c1d2e3f4-a5b6-7890-cdef-1234567890ab",
    "subjectCode": "PRM323",
    "className": "SE1701",
    "semester": "FA24",
    "lecturerId": "l1m2n3o4-p5q6-7890-rstu-vwxyz1234567",
    "lecturerName": null,
    "createdAt": "2026-09-20T10:00:00"
  },
  "timestamp": "2026-09-23T02:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X PUT http://localhost:8080/api/courses/c1d2e3f4-a5b6-7890-cdef-1234567890ab \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "subjectCode": "PRM323",
    "className": "SE1701",
    "semester": "FA24",
    "password": "newPassword456"
  }'
```

---

### 2.5. Xóa khóa học

| Thuộc tính   | Giá trị                              |
|-------------|---------------------------------------|
| **Tên**     | Delete Course                         |
| **Method**  | `DELETE`                              |
| **URL**     | `/api/courses/{id}`                   |
| **Auth**    | ✅ Bearer Token — `LECTURER`, `ADMIN` |

**Path Parameters**:

| Param | Type     | Mô tả          |
|-------|----------|-----------------|
| `id`  | `string` | ID của khóa học |

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Xóa khóa học thành công",
  "data": null,
  "timestamp": "2026-09-23T02:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X DELETE http://localhost:8080/api/courses/c1d2e3f4-a5b6-7890-cdef-1234567890ab \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### 2.6. Tìm kiếm khóa học (Search Courses)

| Thuộc tính   | Giá trị                                               |
|-------------|-------------------------------------------------------|
| **Tên**     | Search Courses                                        |
| **Method**  | `GET`                                                 |
| **URL**     | `/api/courses/search`                                 |
| **Auth**    | ✅ Bearer Token (Tất cả role)                          |

**Query Parameters** *(Tất cả đều là tùy chọn và có thể kết hợp)*:

| Param          | Type     | Required | Mô tả                                               |
|----------------|----------|----------|-----------------------------------------------------|
| `className`    | `string` | ❌       | Tên lớp học phần (tìm kiếm tương đối, VD: `SE1701`) |
| `subjectCode`  | `string` | ❌       | Mã môn học (tìm kiếm tương đối, VD: `PRM392`)       |
| `semester`     | `string` | ❌       | Học kỳ (tìm kiếm tương đối, VD: `FA24`)             |
| `lecturerName` | `string` | ❌       | Họ tên hoặc username giảng viên (tìm kiếm tương đối)|
| `lecturerId`   | `string` | ❌       | ID giảng viên (cho phép lọc cục bộ các lớp của GV)  |

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Tìm kiếm khóa học thành công",
  "data": [
    {
      "courseId": "CRS-001",
      "subjectCode": "PRM392",
      "className": "SE1701",
      "semester": "FA24",
      "lecturerId": "USR-LEC1",
      "lecturerName": "Nguyễn Văn A",
      "createdAt": "2026-09-20T10:00:00"
    }
  ],
  "timestamp": "2026-09-23T02:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
# Tìm kiếm kết hợp theo môn học và giảng viên
curl -X GET "http://localhost:8080/api/courses/search?subjectCode=PRM392&lecturerName=Nguyen" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."

# Tìm kiếm theo lớp và học kỳ
curl -X GET "http://localhost:8080/api/courses/search?className=SE1701&semester=FA24" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."

# Giảng viên tìm kiếm các lớp do chính mình phụ trách
curl -X GET "http://localhost:8080/api/courses/search?lecturerId=USR-LEC1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### 2.7. Học sinh đăng ký khóa học (Register / Enroll Course)

| Thuộc tính   | Giá trị                                               |
|-------------|-------------------------------------------------------|
| **Tên**     | Register Course                                       |
| **Method**  | `POST`                                                |
| **URL**     | `/api/courses/register` (hoặc `/api/courses/enroll`)  |
| **Auth**    | ✅ Bearer Token — `STUDENT`, `ADMIN`                  |

**Request Body** (`application/json`):

| Field            | Type     | Required | Mô tả                                                                    |
|------------------|----------|----------|---------------------------------------------------------------------------|
| `courseId`       | `string` | ✅       | Mã khóa học muốn đăng ký (VD: `CRS-001`)                                  |
| `userId`         | `string` | ✅       | Mã học sinh (`userId` hoặc `username`, alias: `studentId`)                |
| `coursePassword` | `string` | ❌       | Mật khẩu của khóa học (bắt buộc nếu khóa học có mật khẩu, alias: `password`)|

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Đăng ký khóa học thành công",
  "data": {
    "courseId": "CRS-001",
    "subjectCode": "PRM392",
    "className": "SE1701",
    "semester": "FA24",
    "lecturerId": "USR-LEC1",
    "lecturerName": "Nguyễn Văn A",
    "createdAt": "2026-09-20T10:00:00"
  },
  "timestamp": "2026-09-23T02:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X POST http://localhost:8080/api/courses/register \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": "CRS-001",
    "userId": "USR-STU1",
    "coursePassword": "secretPassword123"
  }'
```

---

### 2.8. Lấy danh sách khóa học của học sinh (My Courses)

| Thuộc tính   | Giá trị                                               |
|-------------|-------------------------------------------------------|
| **Tên**     | Get Enrolled Courses                                  |
| **Method**  | `GET`                                                 |
| **URL**     | `/api/courses/my-courses`                             |
| **Auth**    | ✅ Bearer Token — `STUDENT`, `ADMIN`                  |

**Query Parameters** *(Tùy chọn lọc nâng cao)*:

| Param          | Type     | Required | Mô tả                                                                             |
|----------------|----------|----------|-----------------------------------------------------------------------------------|
| `studentId`    | `string` | ❌       | Mã học sinh (`userId`, `username`, `fapCode`). Nếu bỏ trống = lấy từ user login   |
| `subjectCode`  | `string` | ❌       | Lọc theo mã môn học (tìm tương đối, VD: `PRM392`)                                  |
| `semester`     | `string` | ❌       | Lọc theo học kỳ (VD: `FA24`, `SP25`)                                              |
| `lecturerName` | `string` | ❌       | Lọc theo họ tên giảng viên (tìm tương đối)                                        |

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Lấy danh sách khóa học của học sinh thành công",
  "data": [
    {
      "courseId": "CRS-001",
      "subjectCode": "PRM392",
      "className": "SE1701",
      "semester": "FA24",
      "lecturerId": "USR-LEC1",
      "lecturerName": "Nguyễn Văn A",
      "createdAt": "2026-09-20T10:00:00"
    },
    {
      "courseId": "CRS-002",
      "subjectCode": "PRM323",
      "className": "SE1702",
      "semester": "FA24",
      "lecturerId": "USR-LEC2",
      "lecturerName": "Trần Thị B",
      "createdAt": "2026-09-21T14:00:00"
    }
  ],
  "timestamp": "2026-09-23T02:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
# Lấy danh sách khóa học của chính mình
curl -X GET http://localhost:8080/api/courses/my-courses \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."

# Lọc theo môn học và học kỳ
curl -X GET "http://localhost:8080/api/courses/my-courses?subjectCode=PRM392&semester=FA24" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### 2.9. Import danh sách sinh viên vào khóa học qua Excel

| Thuộc tính   | Giá trị                                         |
|-------------|-------------------------------------------------|
| **Tên**     | Import Students to Course                       |
| **Method**  | `POST`                                          |
| **URL**     | `/api/courses/{courseId}/import-students`       |
| **Auth**    | ✅ Bearer Token — `LECTURER`, `ADMIN`           |

**Path Parameters**:

| Param      | Type     | Mô tả                |
|------------|----------|-----------------------|
| `courseId` | `string` | ID của khóa học       |

**Request Body** (`multipart/form-data`):

| Field  | Type            | Required | Mô tả                                               |
|--------|-----------------|----------|------------------------------------------------------|
| `file` | `MultipartFile` | ✅       | File Excel (.xlsx hoặc .xls) chứa danh sách sinh viên |

> 💡 **Quy cách file Excel**:
> Hệ thống tự động nhận diện các cột theo tiêu đề: `RollNumber` (hoặc `FAPCode`, `MSSV`), `Email`, `MemberCode`, `FullName`.
> Với mỗi sinh viên, nếu chưa có tài khoản sẽ tự động tạo tài khoản mặc định (mật khẩu mặc định `A123456`) và ghi danh vào lớp học phần.

**Response** (`201 Created`):

```json
{
  "success": true,
  "message": "Import danh sách sinh viên vào khóa học thành công",
  "data": {
    "courseId": "CRS-001",
    "totalRows": 35,
    "successCount": 33,
    "skippedCount": 2,
    "errors": []
  },
  "timestamp": "2026-09-24T08:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X POST http://localhost:8080/api/courses/CRS-001/import-students \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -F "file=@danh_sach_sinh_vien.xlsx"
```

---

## 3. 📝 Enrollment Controller

**Base path**: `/api/enrollments`

---

### 3.1. Đăng ký tham gia khóa học

| Thuộc tính   | Giá trị                              |
|-------------|---------------------------------------|
| **Tên**     | Enroll Course                         |
| **Method**  | `POST`                                |
| **URL**     | `/api/enrollments`                    |
| **Auth**    | ✅ Bearer Token — `STUDENT`, `ADMIN`  |

**Request Body** (`application/json`):

| Field       | Type     | Required | Mô tả                                                 |
|------------|----------|----------|--------------------------------------------------------|
| `courseId`  | `string` | ✅       | ID khóa học muốn đăng ký                               |
| `studentId`| `string` | ❌       | ID sinh viên (nếu bỏ trống = lấy từ user đang login)   |

**Response** (`201 Created`):

```json
{
  "success": true,
  "message": "Đăng ký khóa học thành công",
  "data": {
    "enrollmentId": "e1f2g3h4-i5j6-7890-klmn-opqrstuvwxyz",
    "courseId": "c1d2e3f4-a5b6-7890-cdef-1234567890ab",
    "studentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "enrolledAt": "2026-09-23T09:00:00",
    "subjectCode": "PRM392",
    "className": "SE1701",
    "semester": "FA24",
    "studentName": "Ngô Hoàng Trường Đạt",
    "fapCode": "SE192964"
  },
  "timestamp": "2026-09-23T02:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X POST http://localhost:8080/api/enrollments \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": "c1d2e3f4-a5b6-7890-cdef-1234567890ab"
  }'
```

---

### 3.2. Xem danh sách khóa học đã đăng ký (My Courses)

| Thuộc tính   | Giá trị                              |
|-------------|---------------------------------------|
| **Tên**     | Get My Enrollments                    |
| **Method**  | `GET`                                 |
| **URL**     | `/api/enrollments/my-courses`         |
| **Auth**    | ✅ Bearer Token — `STUDENT`, `ADMIN`  |

**Request**: Không có body hay params (lấy từ JWT Token).

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Lấy danh sách khóa học đã đăng ký thành công",
  "data": [
    {
      "enrollmentId": "e1f2g3h4-i5j6-7890-klmn-opqrstuvwxyz",
      "courseId": "c1d2e3f4-a5b6-7890-cdef-1234567890ab",
      "studentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "enrolledAt": "2026-09-23T09:00:00",
      "subjectCode": "PRM392",
      "className": "SE1701",
      "semester": "FA24",
      "studentName": "Ngô Hoàng Trường Đạt",
      "fapCode": "SE192964"
    }
  ],
  "timestamp": "2026-09-23T02:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X GET http://localhost:8080/api/enrollments/my-courses \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### 3.3. Danh sách sinh viên trong lớp học

| Thuộc tính   | Giá trị                              |
|-------------|---------------------------------------|
| **Tên**     | Get Students By Course                |
| **Method**  | `GET`                                 |
| **URL**     | `/api/enrollments/course/{courseId}`   |
| **Auth**    | ✅ Bearer Token — `LECTURER`, `ADMIN` |

**Path Parameters**:

| Param      | Type     | Mô tả          |
|-----------|----------|-----------------|
| `courseId` | `string` | ID của khóa học |

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Lấy danh sách sinh viên trong lớp học thành công",
  "data": [
    {
      "enrollmentId": "e1f2g3h4-i5j6-7890-klmn-opqrstuvwxyz",
      "courseId": "c1d2e3f4-a5b6-7890-cdef-1234567890ab",
      "studentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "enrolledAt": "2026-09-23T09:00:00",
      "subjectCode": "PRM392",
      "className": "SE1701",
      "semester": "FA24",
      "studentName": "Ngô Hoàng Trường Đạt",
      "fapCode": "SE192964"
    }
  ],
  "timestamp": "2026-09-23T02:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X GET http://localhost:8080/api/enrollments/course/c1d2e3f4-a5b6-7890-cdef-1234567890ab \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## 4. 🗓️ Attendance Session Controller

**Base path**: `/api/sessions`

---

### 4.1. Tạo buổi điểm danh

| Thuộc tính   | Giá trị                              |
|-------------|---------------------------------------|
| **Tên**     | Create Session                        |
| **Method**  | `POST`                                |
| **URL**     | `/api/sessions`                       |
| **Auth**    | ✅ Bearer Token — `LECTURER`, `ADMIN` |

**Request Body** (`application/json`):

| Field           | Type       | Required | Validation        | Mô tả                                    |
|-----------------|-----------|----------|-------------------|-------------------------------------------|
| `courseId`       | `string`  | ✅       | Không được trống   | ID khóa học                               |
| `slotNumber`     | `integer` | ✅       | 1–6               | Tiết học (Slot 1 đến Slot 6)              |
| `expiredAt`      | `string`  | ✅       | ISO 8601 datetime  | Thời gian hết hạn QR (`yyyy-MM-ddTHH:mm:ss`)|
| `roomNetworkIp`  | `string`  | ❌       |                    | IP mạng phòng học (kiểm tra cùng mạng)     |

**Response** (`201 Created`):

```json
{
  "success": true,
  "message": "Tạo buổi điểm danh thành công",
  "data": {
    "sessionId": "s1t2u3v4-w5x6-7890-yzab-cdef12345678",
    "courseId": "c1d2e3f4-a5b6-7890-cdef-1234567890ab",
    "slotNumber": 1,
    "sessionCode": "849201",
    "qrCodeSecret": "ATTENDANCE_QR_a1b2c3d4e5f678901234567890abcdef",
    "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAAAPoAAAD6...",
    "roomNetworkIp": "192.168.1.0",
    "expiredAt": "2026-09-23T10:30:00",
    "status": "OPEN",
    "createdAt": "2026-09-23T09:30:00"
  },
  "timestamp": "2026-09-23T02:30:00.000Z"
}
```

> 💡 **Lưu ý**:
> - `sessionCode`: Mã PIN 6 chữ số ngẫu nhiên cho phép sinh viên nhập thủ công thay vì quét QR.
> - `qrCodeBase64` là hình ảnh QR code mã hóa Base64 (PNG). Frontend hiển thị bằng:
> ```html
> <img src="data:image/png;base64,{qrCodeBase64}" />
> ```

**Ví dụ Request**:
```bash
curl -X POST http://localhost:8080/api/sessions \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": "c1d2e3f4-a5b6-7890-cdef-1234567890ab",
    "slotNumber": 1,
    "expiredAt": "2026-09-23T10:30:00",
    "roomNetworkIp": "192.168.1.0"
  }'
```

---

### 4.2. Xem chi tiết buổi điểm danh

| Thuộc tính   | Giá trị                              |
|-------------|---------------------------------------|
| **Tên**     | Get Session By ID                     |
| **Method**  | `GET`                                 |
| **URL**     | `/api/sessions/{sessionId}`           |
| **Auth**    | ✅ Bearer Token (Tất cả role)         |

**Path Parameters**:

| Param       | Type     | Mô tả                |
|------------|----------|-----------------------|
| `sessionId`| `string` | ID buổi điểm danh     |

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Lấy thông tin buổi điểm danh thành công",
  "data": {
    "sessionId": "s1t2u3v4-w5x6-7890-yzab-cdef12345678",
    "courseId": "c1d2e3f4-a5b6-7890-cdef-1234567890ab",
    "slotNumber": 1,
    "sessionCode": "849201",
    "qrCodeSecret": "ATTENDANCE_QR_a1b2c3d4e5f678901234567890abcdef",
    "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAAAPoAAAD6...",
    "roomNetworkIp": "192.168.1.0",
    "expiredAt": "2026-09-23T10:30:00",
    "status": "OPEN",
    "createdAt": "2026-09-23T09:30:00"
  },
  "timestamp": "2026-09-23T02:30:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X GET http://localhost:8080/api/sessions/s1t2u3v4-w5x6-7890-yzab-cdef12345678 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### 4.3. Danh sách buổi điểm danh theo khóa học

| Thuộc tính   | Giá trị                              |
|-------------|---------------------------------------|
| **Tên**     | Get Sessions By Course                |
| **Method**  | `GET`                                 |
| **URL**     | `/api/sessions/course/{courseId}`      |
| **Auth**    | ✅ Bearer Token (Tất cả role)         |

**Path Parameters**:

| Param      | Type     | Mô tả          |
|-----------|----------|-----------------|
| `courseId` | `string` | ID của khóa học |

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Lấy danh sách buổi điểm danh thành công",
  "data": [
    {
      "sessionId": "s1t2u3v4-w5x6-7890-yzab-cdef12345678",
      "courseId": "c1d2e3f4-a5b6-7890-cdef-1234567890ab",
      "slotNumber": 1,
      "qrCodeSecret": "ATTENDANCE_QR_...",
      "qrCodeBase64": "iVBORw0KGgo...",
      "roomNetworkIp": "192.168.1.0",
      "expiredAt": "2026-09-23T10:30:00",
      "status": "OPEN",
      "createdAt": "2026-09-23T09:30:00"
    }
  ],
  "timestamp": "2026-09-23T02:30:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X GET http://localhost:8080/api/sessions/course/c1d2e3f4-a5b6-7890-cdef-1234567890ab \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### 4.4. Đóng buổi điểm danh

| Thuộc tính   | Giá trị                              |
|-------------|---------------------------------------|
| **Tên**     | Close Session                         |
| **Method**  | `PUT`                                 |
| **URL**     | `/api/sessions/{sessionId}/close`     |
| **Auth**    | ✅ Bearer Token — `LECTURER`, `ADMIN` |

**Path Parameters**:

| Param       | Type     | Mô tả                |
|------------|----------|-----------------------|
| `sessionId`| `string` | ID buổi điểm danh     |

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Đóng buổi điểm danh thành công",
  "data": {
    "sessionId": "s1t2u3v4-w5x6-7890-yzab-cdef12345678",
    "courseId": "c1d2e3f4-a5b6-7890-cdef-1234567890ab",
    "slotNumber": 1,
    "qrCodeSecret": "ATTENDANCE_QR_...",
    "qrCodeBase64": null,
    "roomNetworkIp": "192.168.1.0",
    "expiredAt": "2026-09-23T10:30:00",
    "status": "CLOSED",
    "createdAt": "2026-09-23T09:30:00"
  },
  "timestamp": "2026-09-23T03:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X PUT http://localhost:8080/api/sessions/s1t2u3v4-w5x6-7890-yzab-cdef12345678/close \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## 5. ✅ Attendance Controller

**Base path**: `/api/attendance`

---

### 5.1. Sinh viên điểm danh qua QR hoặc mã PIN (Session Code)

| Thuộc tính   | Giá trị                              |
|-------------|---------------------------------------|
| **Tên**     | Check In (QR Attendance / PIN Code)   |
| **Method**  | `POST`                                |
| **URL**     | `/api/attendance/check-in`            |
| **Auth**    | ✅ Bearer Token — `STUDENT`, `ADMIN`  |

**Request Body** (`application/json`):

| Field          | Type     | Required | Mô tả                                                                    |
|---------------|----------|----------|---------------------------------------------------------------------------|
| `sessionId`    | `string` | ⚠️       | ID buổi điểm danh (bắt buộc nếu điểm danh qua QR; tùy chọn nếu dùng PIN)  |
| `qrCodeSecret` | `string` | ⚠️       | Mã bí mật QR Code (bắt buộc nếu quét QR)                                  |
| `sessionCode`  | `string` | ⚠️       | Mã PIN 6 chữ số buổi học (thay thế cho QR; tìm tự động session đang OPEN)|
| `clientIp`     | `string` | ❌       | IP thiết bị (nếu bỏ trống hệ thống tự lấy qua header `X-Forwarded-For`)   |

> ⚠️ **Hệ thống kiểm tra 6 điều kiện nghiệp vụ**:
> 1. Session tồn tại và đang `OPEN` (theo `sessionId` hoặc `sessionCode`)
> 2. QR code secret khớp (nếu quét QR) HOẶC sessionCode 6 chữ số hợp lệ (nếu nhập PIN)
> 3. Buổi học chưa hết hạn (`expiredAt`)
> 4. Sinh viên đã đăng ký khóa học (`enrollment`)
> 5. Chưa điểm danh trùng (chống spam / điểm danh lặp)
> 6. **Xác thực Wi-Fi phòng học**: Nếu buổi học cấu hình `roomNetworkIp`, địa chỉ IP của sinh viên phải cùng mạng (cùng IP hoặc cùng subnet `/24`, ví dụ `192.168.1.*`)

**Response** (`201 Created`):

```json
{
  "success": true,
  "message": "Điểm danh thành công",
  "data": {
    "recordId": "r1s2t3u4-v5w6-7890-xyza-bcdef1234567",
    "sessionId": "s1t2u3v4-w5x6-7890-yzab-cdef12345678",
    "studentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "studentFapCode": "SE192964",
    "studentName": "Ngô Hoàng Trường Đạt",
    "status": "PRESENT",
    "checkedAt": "2026-09-23T09:45:00"
  },
  "timestamp": "2026-09-23T02:45:00.000Z"
}
```

**Ví dụ Request 1 (Quét mã QR)**:
```bash
curl -X POST http://localhost:8080/api/attendance/check-in \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "s1t2u3v4-w5x6-7890-yzab-cdef12345678",
    "qrCodeSecret": "ATTENDANCE_QR_a1b2c3d4e5f678901234567890abcdef"
  }'
```

**Ví dụ Request 2 (Nhập mã PIN 6 số)**:
```bash
curl -X POST http://localhost:8080/api/attendance/check-in \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "sessionCode": "849201"
  }'
```

---

### 5.2. Xem lịch sử điểm danh của buổi học

| Thuộc tính   | Giá trị                                   |
|-------------|---------------------------------------------|
| **Tên**     | Get Attendance Records By Session            |
| **Method**  | `GET`                                        |
| **URL**     | `/api/attendance/session/{sessionId}`        |
| **Auth**    | ✅ Bearer Token — `LECTURER`, `ADMIN`        |

**Path Parameters**:

| Param       | Type     | Mô tả                |
|------------|----------|-----------------------|
| `sessionId`| `string` | ID buổi điểm danh     |

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Lấy lịch sử điểm danh thành công",
  "data": [
    {
      "recordId": "r1s2t3u4-v5w6-7890-xyza-bcdef1234567",
      "sessionId": "s1t2u3v4-w5x6-7890-yzab-cdef12345678",
      "studentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "studentFapCode": "SE192964",
      "studentName": "Ngô Hoàng Trường Đạt",
      "status": "PRESENT",
      "checkedAt": "2026-09-23T09:45:00"
    }
  ],
  "timestamp": "2026-09-23T02:45:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X GET http://localhost:8080/api/attendance/session/s1t2u3v4-w5x6-7890-yzab-cdef12345678 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### 5.3. Xuất danh sách điểm danh ra CSV

| Thuộc tính   | Giá trị                                    |
|-------------|----------------------------------------------|
| **Tên**     | Export Attendance to CSV                      |
| **Method**  | `GET`                                         |
| **URL**     | `/api/attendance/export/{sessionId}`          |
| **Auth**    | ✅ Bearer Token — `LECTURER`, `ADMIN`         |

**Path Parameters**:

| Param       | Type     | Mô tả                |
|------------|----------|-----------------------|
| `sessionId`| `string` | ID buổi điểm danh     |

**Response** (`200 OK`):

- **Content-Type**: `text/csv; charset=UTF-8`
- **Content-Disposition**: `attachment; filename="attendance_session_{sessionId}.csv"`
- **Body**: File CSV binary

> 💡 **Lưu ý cho Frontend**: Endpoint này trả về file binary, **không phải JSON**. Xử lý download file:
> ```javascript
> fetch('/api/attendance/export/' + sessionId, {
>   headers: { 'Authorization': 'Bearer ' + token }
> })
> .then(res => res.blob())
> .then(blob => {
>   const url = URL.createObjectURL(blob);
>   const a = document.createElement('a');
>   a.href = url;
>   a.download = `attendance_session_${sessionId}.csv`;
>   a.click();
> });
> ```

**Ví dụ Request**:
```bash
curl -X GET http://localhost:8080/api/attendance/export/s1t2u3v4-w5x6-7890-yzab-cdef12345678 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -o attendance_report.csv
```

---

### 5.4. Lịch sử điểm danh của cá nhân sinh viên

| Thuộc tính   | Giá trị                                      |
|-------------|-----------------------------------------------|
| **Tên**     | Get My Attendance History                     |
| **Method**  | `GET`                                         |
| **URL**     | `/api/attendance/my-history`                  |
| **Auth**    | ✅ Bearer Token — `STUDENT`, `ADMIN`          |

**Query Parameters**:

| Param      | Type     | Required | Mô tả                                               |
|------------|----------|----------|------------------------------------------------------|
| `courseId` | `string` | ❌       | Lọc theo ID khóa học (nếu bỏ trống = lấy tất cả khóa)|

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Lấy lịch sử điểm danh của bạn thành công",
  "data": [
    {
      "recordId": "r1s2t3u4-v5w6-7890-xyza-bcdef1234567",
      "sessionId": "s1t2u3v4-w5x6-7890-yzab-cdef12345678",
      "courseId": "c1d2e3f4-a5b6-7890-cdef-1234567890ab",
      "subjectCode": "PRM392",
      "className": "SE1701",
      "slotNumber": 1,
      "status": "PRESENT",
      "checkedAt": "2026-09-23T09:45:00"
    }
  ],
  "timestamp": "2026-09-24T08:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
# Xem tất cả
curl -X GET http://localhost:8080/api/attendance/my-history \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."

# Lọc theo khóa học
curl -X GET "http://localhost:8080/api/attendance/my-history?courseId=c1d2e3f4-a5b6-7890-cdef-1234567890ab" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

### 5.5. Xuất danh sách điểm danh ra file Excel (.xlsx)

| Thuộc tính   | Giá trị                                      |
|-------------|-----------------------------------------------|
| **Tên**     | Export Attendance to Excel                    |
| **Method**  | `GET`                                         |
| **URL**     | `/api/attendance/export-excel/{sessionId}`    |
| **Auth**    | ✅ Bearer Token — `LECTURER`, `ADMIN`         |

> 💡 **Đặc điểm nổi bật**: Báo cáo tổng hợp **100% sinh viên** có trong danh sách lớp (`enrollment`). Sinh viên đã quét mã/nhập PIN ghi nhận trạng thái `PRESENT` kèm thời gian điểm danh; sinh viên không điểm danh tự động ghi nhận trạng thái `ABSENT`.

**Path Parameters**:

| Param       | Type     | Mô tả                |
|------------|----------|-----------------------|
| `sessionId`| `string` | ID buổi điểm danh     |

**Response** (`200 OK`):

- **Content-Type**: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- **Content-Disposition**: `attachment; filename="attendance_session_{sessionId}.xlsx"`
- **Body**: File Excel binary (.xlsx) với các cột: No, Student Code, Full Name, Email, Status, Checked At.

**Ví dụ Request**:
```bash
curl -X GET http://localhost:8080/api/attendance/export-excel/s1t2u3v4-w5x6-7890-yzab-cdef12345678 \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -o attendance_session_report.xlsx
```

---

## 6. 👤 User Controller

**Base path**: `/api/users`

---

### 6.1. Import danh sách người dùng từ file Excel

| Thuộc tính   | Giá trị                              |
|-------------|---------------------------------------|
| **Tên**     | Import Users From Excel               |
| **Method**  | `POST`                                |
| **URL**     | `/api/users/import-excel`             |
| **Auth**    | ❌ Không cần token (Public)            |

**Request Body** (`multipart/form-data`):

| Field  | Type             | Required | Mô tả                                                       |
|--------|------------------|----------|--------------------------------------------------------------|
| `file` | `MultipartFile`  | ✅       | File Excel (.xlsx hoặc .xls)                                 |

**Định dạng file Excel** (các cột theo thứ tự):

| Cột | Tên cột      | Mô tả                 |
|-----|-------------|------------------------|
| A   | Class        | Tên lớp               |
| B   | RollNumber   | Số thứ tự              |
| C   | Email        | Email sinh viên        |
| D   | MemberCode   | Mã thành viên          |
| E   | FullName     | Họ và tên              |
| F   | Password     | Mật khẩu mặc định      |

**Response** (`201 Created`):

```json
{
  "success": true,
  "message": "Import danh sách người dùng từ file Excel thành công",
  "data": [
    {
      "userId": "generated-uuid-1",
      "username": "SE192964",
      "fapCode": "SE192964",
      "fullName": "Ngô Hoàng Trường Đạt",
      "email": "datnhse192964@fpt.edu.vn",
      "roleId": "STUDENT",
      "createdAt": "2026-09-23T09:00:00",
      "memberCode": "HE192964"
    },
    {
      "userId": "generated-uuid-2",
      "username": "SE192965",
      "fapCode": "SE192965",
      "fullName": "Nguyễn Văn A",
      "email": "angvse192965@fpt.edu.vn",
      "roleId": "STUDENT",
      "createdAt": "2026-09-23T09:00:00",
      "memberCode": "HE192965"
    }
  ],
  "timestamp": "2026-09-23T02:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X POST http://localhost:8080/api/users/import-excel \
  -F "file=@danh_sach_sv.xlsx"
```

---

### 6.2. Cập nhật thông tin cá nhân

| Thuộc tính   | Giá trị                              |
|-------------|---------------------------------------|
| **Tên**     | Update Profile                        |
| **Method**  | `PUT`                                 |
| **URL**     | `/api/users/profile`                  |
| **Auth**    | ✅ Bearer Token (Tất cả role)         |

**Request Body** (`application/json`):

| Field      | Type     | Required | Mô tả                                             |
|------------|----------|----------|----------------------------------------------------|
| `fullName` | `string` | ❌       | Họ và tên mới                                      |
| `email`    | `string` | ❌       | Email mới (hệ thống kiểm tra trùng lặp email khác) |

> 💡 **Đồng bộ**: Khi cập nhật thông tin thành công, hệ thống tự động đồng bộ thay đổi sang tab `USERS` trên Google Sheets.

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Cập nhật thông tin thành công",
  "data": {
    "userId": "u1v2w3x4-y5z6-7890-abcd-ef1234567890",
    "username": "SE192964",
    "fapCode": "SE192964",
    "fullName": "Ngô Hoàng Trường Đạt (Updated)",
    "email": "datnhse192964_new@fpt.edu.vn",
    "roleId": "STUDENT",
    "createdAt": "2026-09-23T09:00:00",
    "memberCode": "HE192964"
  },
  "timestamp": "2026-09-24T08:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X PUT http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Ngô Hoàng Trường Đạt (Updated)",
    "email": "datnhse192964_new@fpt.edu.vn"
  }'
```

---

### 6.3. Đổi mật khẩu

| Thuộc tính   | Giá trị                              |
|-------------|---------------------------------------|
| **Tên**     | Change Password                       |
| **Method**  | `PUT`                                 |
| **URL**     | `/api/users/change-password`          |
| **Auth**    | ✅ Bearer Token (Tất cả role)         |

**Request Body** (`application/json`):

| Field         | Type     | Required | Mô tả                                      |
|---------------|----------|----------|---------------------------------------------|
| `oldPassword` | `string` | ✅       | Mật khẩu hiện tại (kiểm tra khớp hash BCrypt)|
| `newPassword` | `string` | ✅       | Mật khẩu mới (tối thiểu 6 ký tự)            |

> 💡 **Đồng bộ**: Mật khẩu mới được mã hóa BCrypt lưu vào Database và đồng bộ sang cột Password trên tab `USERS` Google Sheets.

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Đổi mật khẩu thành công",
  "data": null,
  "timestamp": "2026-09-24T08:00:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X PUT http://localhost:8080/api/users/change-password \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "oldPassword": "oldPassword123",
    "newPassword": "newSecretPassword456"
  }'
```

---

## 7. 📋 Sync Log Controller

**Base path**: `/api/sync-logs`

---

### 7.1. Xem nhật ký đồng bộ

| Thuộc tính   | Giá trị                              |
|-------------|---------------------------------------|
| **Tên**     | Get All Sync Logs                     |
| **Method**  | `GET`                                 |
| **URL**     | `/api/sync-logs`                      |
| **Auth**    | ✅ Bearer Token — `ADMIN`, `LECTURER` |

**Request**: Không có body hay params.

**Response** (`200 OK`):

```json
{
  "success": true,
  "message": "Lấy nhật ký đồng bộ thành công",
  "data": [
    {
      "logId": "log-uuid-1",
      "action": "CREATE_COURSE",
      "status": "SUCCESS",
      "details": "Tạo khóa học PRM392 - SE1701 thành công trên Google Sheets",
      "syncedAt": "2026-09-23T09:00:00"
    },
    {
      "logId": "log-uuid-2",
      "action": "CHECK_IN",
      "status": "SUCCESS",
      "details": "Sinh viên SE192964 điểm danh session xyz thành công",
      "syncedAt": "2026-09-23T09:45:00"
    }
  ],
  "timestamp": "2026-09-23T02:45:00.000Z"
}
```

**Ví dụ Request**:
```bash
curl -X GET http://localhost:8080/api/sync-logs \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## 📊 Tổng hợp tất cả Endpoints

| #  | Method   | URL                                            | Auth         | Role                 | Mô tả                                                |
|----|----------|------------------------------------------------|-------------|----------------------|-------------------------------------------------------|
| 1  | `POST`   | `/api/auth/register`                           | ❌ Public   | —                    | Đăng ký tài khoản                                     |
| 2  | `POST`   | `/api/auth/login`                              | ❌ Public   | —                    | Đăng nhập, lấy JWT Token                              |
| 3  | `GET`    | `/api/courses`                                 | ✅ Token    | Tất cả               | Lấy danh sách khóa học (lọc lecturerId)               |
| 4  | `GET`    | `/api/courses/{id}`                            | ✅ Token    | Tất cả               | Xem chi tiết khóa học                                 |
| 5  | `POST`   | `/api/courses`                                 | ✅ Token    | LECTURER, ADMIN      | Tạo khóa học mới                                      |
| 6  | `PUT`    | `/api/courses/{id}`                            | ✅ Token    | LECTURER, ADMIN      | Cập nhật khóa học                                     |
| 7  | `DELETE` | `/api/courses/{id}`                            | ✅ Token    | LECTURER, ADMIN      | Xóa khóa học                                          |
| 8  | `GET`    | `/api/courses/search`                          | ✅ Token    | Tất cả               | Tìm kiếm khóa học nâng cao (subject, class, sem, lec) |
| 9  | `GET`    | `/api/courses/my-courses`                      | ✅ Token    | STUDENT, ADMIN       | Xem khóa học đã tham gia (hỗ trợ lọc semester/subject)|
| 10 | `POST`   | `/api/courses/{courseId}/enroll`               | ✅ Token    | STUDENT, ADMIN       | Sinh viên đăng ký tham gia khóa học                   |
| 11 | `POST`   | `/api/courses/{courseId}/import-students`      | ✅ Token    | LECTURER, ADMIN      | Import danh sách sinh viên vào khóa học bằng Excel    |
| 12 | `POST`   | `/api/enrollments`                             | ✅ Token    | STUDENT, ADMIN       | Đăng ký tham gia khóa học (EnrollmentController)      |
| 13 | `GET`    | `/api/enrollments/my-courses`                  | ✅ Token    | STUDENT, ADMIN       | Xem khóa học đã đăng ký (EnrollmentController)        |
| 14 | `GET`    | `/api/enrollments/course/{courseId}`            | ✅ Token    | LECTURER, ADMIN      | DS sinh viên trong lớp                                |
| 15 | `POST`   | `/api/sessions`                                | ✅ Token    | LECTURER, ADMIN      | Tạo buổi điểm danh (sinh QR & mã PIN 6 số)            |
| 16 | `GET`    | `/api/sessions/{sessionId}`                    | ✅ Token    | Tất cả               | Chi tiết buổi điểm danh                               |
| 17 | `GET`    | `/api/sessions/course/{courseId}`               | ✅ Token    | Tất cả               | DS buổi điểm danh theo khóa học                       |
| 18 | `PUT`    | `/api/sessions/{sessionId}/close`              | ✅ Token    | LECTURER, ADMIN      | Đóng buổi điểm danh                                  |
| 19 | `POST`   | `/api/attendance/check-in`                     | ✅ Token    | STUDENT, ADMIN       | Điểm danh qua QR / mã PIN / kiểm tra Wi-Fi            |
| 20 | `GET`    | `/api/attendance/session/{sessionId}`          | ✅ Token    | LECTURER, ADMIN      | Lịch sử điểm danh buổi học                            |
| 21 | `GET`    | `/api/attendance/my-history`                   | ✅ Token    | STUDENT, ADMIN       | Lịch sử điểm danh cá nhân sinh viên                   |
| 22 | `GET`    | `/api/attendance/export/{sessionId}`           | ✅ Token    | LECTURER, ADMIN      | Xuất CSV điểm danh                                    |
| 23 | `GET`    | `/api/attendance/export-excel/{sessionId}`     | ✅ Token    | LECTURER, ADMIN      | Xuất Excel 100% sinh viên (PRESENT / ABSENT)          |
| 24 | `POST`   | `/api/users/import-excel`                      | ❌ Public   | —                    | Import danh sách người dùng từ Excel                  |
| 25 | `PUT`    | `/api/users/profile`                           | ✅ Token    | Tất cả               | Cập nhật thông tin cá nhân (sync Sheets)              |
| 26 | `PUT`    | `/api/users/change-password`                   | ✅ Token    | Tất cả               | Đổi mật khẩu tài khoản (sync Sheets)                  |
| 27 | `GET`    | `/api/sync-logs`                               | ✅ Token    | ADMIN, LECTURER      | Xem nhật ký đồng bộ Google Sheets                     |

---

## ⚠️ Mã lỗi thường gặp

| HTTP Status | Mô tả                                                 | Nguyên nhân thường gặp                           |
|-------------|--------------------------------------------------------|-------------------------------------------------|
| `400`       | Bad Request — Dữ liệu không hợp lệ                   | Thiếu field bắt buộc, sai format, đăng ký trùng |
| `401`       | Unauthorized — Chưa đăng nhập                          | Thiếu/sai JWT Token, token hết hạn               |
| `403`       | Forbidden — Không có quyền                             | Role không đủ quyền truy cập endpoint            |
| `404`       | Not Found — Không tìm thấy tài nguyên                  | ID khóa học/session không tồn tại                |
| `500`       | Internal Server Error — Lỗi hệ thống                  | Lỗi kết nối DB, Google Sheets, lỗi logic        |
| `503`       | Service Unavailable — Dịch vụ không khả dụng           | Google Sheets không kết nối được                 |
