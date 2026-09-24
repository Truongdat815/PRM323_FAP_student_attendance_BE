package com.example.attendance_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO trả về kết quả import danh sách sinh viên vào khóa học qua Excel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportStudentsResponse {

    private String courseId;
    private int totalRows;
    private int successCount;
    private int skippedCount;
    private List<String> errors;
}
