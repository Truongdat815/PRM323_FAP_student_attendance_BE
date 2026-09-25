package com.example.attendance_backend.service;

import com.example.attendance_backend.dto.response.UserResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ExcelImportService {

    List<UserResponse> importUsersFromExcel(MultipartFile file) throws IOException;
}
