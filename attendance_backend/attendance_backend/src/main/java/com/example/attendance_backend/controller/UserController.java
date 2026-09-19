package com.example.attendance_backend.controller;

import com.example.attendance_backend.dto.response.ApiResponse;
import com.example.attendance_backend.dto.response.UserResponse;
import com.example.attendance_backend.service.ExcelImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Controller tiếp nhận các yêu cầu quản lý người dùng và import dữ liệu Excel.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "User Controller", description = "Các API quản lý người dùng và import dữ liệu Excel")
public class UserController {

    private final ExcelImportService excelImportService;

    @Autowired
    public UserController(ExcelImportService excelImportService) {
        this.excelImportService = excelImportService;
    }

    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import danh sách người dùng từ file Excel", description = "Tải lên file Excel (.xlsx hoặc .xls) theo định dạng (Class, RollNumber, Email, MemberCode, FullName, Password) để lưu vào cơ sở dữ liệu")
    public ResponseEntity<ApiResponse<List<UserResponse>>> importUsersFromExcel(@RequestParam("file") MultipartFile file) throws IOException {
        List<UserResponse> responses = excelImportService.importUsersFromExcel(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Import danh sách người dùng từ file Excel thành công", responses));
    }
}
