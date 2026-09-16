package com.example.attendance_backend.service;

import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;

import java.io.IOException;
import java.util.List;

/**
 * Interface định nghĩa các thao tác giao tiếp với Google Sheets API.
 */
public interface GoogleSheetsService {

    /**
     * Kiểm tra trạng thái kết nối Google Sheets client.
     */
    boolean isConnected();

    /**
     * Lấy ID bảng tính mặc định được cấu hình trong application properties.
     */
    String getDefaultSpreadsheetId();

    /**
     * Đọc dữ liệu từ một dải ô cụ thể theo spreadsheetId.
     */
    List<List<Object>> readRange(String spreadsheetId, String range) throws IOException;

    /**
     * Đọc dữ liệu từ một dải ô theo spreadsheetId mặc định.
     */
    List<List<Object>> readRange(String range) throws IOException;

    /**
     * Đọc toàn bộ dữ liệu của một sheet (bỏ qua header).
     */
    List<List<Object>> readSheet(String sheetName) throws IOException;

    /**
     * Ghi thêm một dòng dữ liệu vào cuối sheet.
     */
    AppendValuesResponse appendRow(String spreadsheetId, String sheetName, List<Object> rowValues) throws IOException;

    /**
     * Ghi thêm một dòng dữ liệu vào cuối sheet sử dụng spreadsheetId mặc định.
     */
    AppendValuesResponse appendRow(String sheetName, List<Object> rowValues) throws IOException;

    /**
     * Cập nhật dữ liệu vào dải ô chỉ định.
     */
    UpdateValuesResponse updateRow(String spreadsheetId, String range, List<Object> rowValues) throws IOException;

    /**
     * Cập nhật dữ liệu vào dải ô chỉ định sử dụng spreadsheetId mặc định.
     */
    UpdateValuesResponse updateRow(String range, List<Object> rowValues) throws IOException;

    /**
     * Tìm số thứ tự dòng thực tế trên Google Sheets (1-indexed) theo giá trị cột.
     */
    int findRowIndex(String spreadsheetId, String sheetName, int columnIndex, String searchValue) throws IOException;

    /**
     * Lọc và lấy tất cả các dòng khớp với giá trị tại cột chỉ định.
     */
    List<List<Object>> findRows(String spreadsheetId, String sheetName, int columnIndex, String searchValue) throws IOException;

    /**
     * Xóa sạch một dòng theo số thứ tự dòng (1-indexed) trên Google Sheets.
     */
    void deleteRow(String spreadsheetId, String sheetName, int rowIndex) throws IOException;

    /**
     * Xóa một dòng sử dụng spreadsheetId mặc định.
     */
    void deleteRow(String sheetName, int rowIndex) throws IOException;
}
