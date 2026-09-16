package com.example.attendance_backend.service;

import com.example.attendance_backend.util.SheetConstants;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lớp tiện ích đóng vai trò Database Access Layer, thực hiện các thao tác
 * đọc, ghi, cập nhật dữ liệu trực tiếp trên Google Sheets.
 */
@Service
public class GoogleSheetsService {

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsService.class);

    private final Sheets sheetsService;

    @Value("${google.sheets.spreadsheet-id:}")
    private String defaultSpreadsheetId;

    @Autowired
    public GoogleSheetsService(@Autowired(required = false) Sheets sheetsService) {
        this.sheetsService = sheetsService;
    }

    /**
     * Kiểm tra kết nối Google Sheets Client đã sẵn sàng hay chưa.
     */
    public boolean isConnected() {
        return sheetsService != null;
    }

    private void ensureConnected() {
        if (!isConnected()) {
            throw new IllegalStateException("Google Sheets Client chưa được cấu hình hoặc thiếu file credentials.json trong src/main/resources/");
        }
    }

    public String getDefaultSpreadsheetId() {
        return defaultSpreadsheetId;
    }

    /**
     * Đọc dữ liệu từ một dải ô cụ thể trên Google Sheets.
     *
     * @param spreadsheetId ID của Google Spreadsheet
     * @param range         Dải ô (ví dụ: "USERS!A2:Z")
     * @return Danh sách các dòng dữ liệu (mỗi dòng là một List<Object>)
     */
    public List<List<Object>> readRange(String spreadsheetId, String range) throws IOException {
        ensureConnected();
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

        List<List<Object>> values = response.getValues();
        return values != null ? values : Collections.emptyList();
    }

    /**
     * Đọc dữ liệu từ một dải ô bằng Spreadsheet ID mặc định.
     */
    public List<List<Object>> readRange(String range) throws IOException {
        return readRange(defaultSpreadsheetId, range);
    }

    /**
     * Đọc toàn bộ dữ liệu của một Sheet (bỏ qua dòng tiêu đề dòng 1).
     */
    public List<List<Object>> readSheet(String sheetName) throws IOException {
        return readRange(defaultSpreadsheetId, SheetConstants.dataRange(sheetName));
    }

    /**
     * Ghi thêm một dòng dữ liệu mới vào cuối sheet.
     *
     * @param spreadsheetId ID của Google Spreadsheet
     * @param sheetName     Tên Tab cần ghi (ví dụ: "USERS")
     * @param rowValues     Các giá trị tương ứng của dòng
     * @return Kết quả AppendValuesResponse từ Google Sheets API
     */
    public AppendValuesResponse appendRow(String spreadsheetId, String sheetName, List<Object> rowValues) throws IOException {
        ensureConnected();
        ValueRange body = new ValueRange().setValues(Collections.singletonList(rowValues));
        return sheetsService.spreadsheets().values()
                .append(spreadsheetId, sheetName, body)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute();
    }

    /**
     * Ghi thêm dòng vào sheet sử dụng Spreadsheet ID mặc định.
     */
    public AppendValuesResponse appendRow(String sheetName, List<Object> rowValues) throws IOException {
        return appendRow(defaultSpreadsheetId, sheetName, rowValues);
    }

    /**
     * Cập nhật dữ liệu vào một dải ô cụ thể (ví dụ: "USERS!A5:G5").
     */
    public UpdateValuesResponse updateRow(String spreadsheetId, String range, List<Object> rowValues) throws IOException {
        ensureConnected();
        ValueRange body = new ValueRange().setValues(Collections.singletonList(rowValues));
        return sheetsService.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }

    /**
     * Cập nhật dòng sử dụng Spreadsheet ID mặc định.
     */
    public UpdateValuesResponse updateRow(String range, List<Object> rowValues) throws IOException {
        return updateRow(defaultSpreadsheetId, range, rowValues);
    }

    /**
     * Tìm kiếm chỉ số dòng thực tế (1-indexed theo Google Sheets) theo điều kiện giá trị ở một cột.
     * Ví dụ: Tìm username ở cột index 1 trong sheet USERS.
     *
     * @param spreadsheetId ID của Google Spreadsheet
     * @param sheetName     Tên Tab
     * @param columnIndex   Chỉ số cột (0-indexed: cột A là 0, B là 1, ...)
     * @param searchValue   Giá trị cần tìm kiếm
     * @return Chỉ số dòng trên Google Sheet (bắt đầu từ dòng 2), trả về -1 nếu không tìm thấy
     */
    public int findRowIndex(String spreadsheetId, String sheetName, int columnIndex, String searchValue) throws IOException {
        List<List<Object>> rows = readSheet(sheetName);
        for (int i = 0; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (row.size() > columnIndex) {
                Object val = row.get(columnIndex);
                if (val != null && val.toString().trim().equalsIgnoreCase(searchValue.trim())) {
                    // Dòng 1 là Header, nên dòng dữ liệu đầu tiên (i = 0) là dòng 2 trên Google Sheet
                    return i + 2;
                }
            }
        }
        return -1;
    }

    /**
     * Tìm tất cả các dòng khớp với giá trị tại một cột cụ thể.
     */
    public List<List<Object>> findRows(String spreadsheetId, String sheetName, int columnIndex, String searchValue) throws IOException {
        List<List<Object>> rows = readSheet(sheetName);
        List<List<Object>> matches = new ArrayList<>();
        for (List<Object> row : rows) {
            if (row.size() > columnIndex) {
                Object val = row.get(columnIndex);
                if (val != null && val.toString().trim().equalsIgnoreCase(searchValue.trim())) {
                    matches.add(row);
                }
            }
        }
        return matches;
    }
}
