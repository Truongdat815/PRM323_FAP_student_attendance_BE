package com.example.attendance_backend.service.impl;

import com.example.attendance_backend.service.GoogleSheetsService;
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
 * Lớp triển khai cụ thể của GoogleSheetsService, trực tiếp gọi Google Sheets API v4.
 */
@Service
public class GoogleSheetsServiceImpl implements GoogleSheetsService {

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsServiceImpl.class);

    private final Sheets sheetsService;

    @Value("${google.sheets.spreadsheet-id:}")
    private String defaultSpreadsheetId;

    @Autowired
    public GoogleSheetsServiceImpl(@Autowired(required = false) Sheets sheetsService) {
        this.sheetsService = sheetsService;
    }

    @Override
    public boolean isConnected() {
        return sheetsService != null;
    }

    private void ensureConnected() {
        if (!isConnected()) {
            throw new IllegalStateException("Google Sheets Client chưa được cấu hình hoặc thiếu file credentials.json trong src/main/resources/");
        }
    }

    @Override
    public String getDefaultSpreadsheetId() {
        return defaultSpreadsheetId;
    }

    @Override
    public List<List<Object>> readRange(String spreadsheetId, String range) throws IOException {
        ensureConnected();
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

        List<List<Object>> values = response.getValues();
        return values != null ? values : Collections.emptyList();
    }

    @Override
    public List<List<Object>> readRange(String range) throws IOException {
        return readRange(defaultSpreadsheetId, range);
    }

    @Override
    public List<List<Object>> readSheet(String sheetName) throws IOException {
        return readRange(defaultSpreadsheetId, SheetConstants.dataRange(sheetName));
    }

    @Override
    public AppendValuesResponse appendRow(String spreadsheetId, String sheetName, List<Object> rowValues) throws IOException {
        ensureConnected();
        ValueRange body = new ValueRange().setValues(Collections.singletonList(rowValues));
        return sheetsService.spreadsheets().values()
                .append(spreadsheetId, sheetName, body)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute();
    }

    @Override
    public AppendValuesResponse appendRow(String sheetName, List<Object> rowValues) throws IOException {
        return appendRow(defaultSpreadsheetId, sheetName, rowValues);
    }

    @Override
    public UpdateValuesResponse updateRow(String spreadsheetId, String range, List<Object> rowValues) throws IOException {
        ensureConnected();
        ValueRange body = new ValueRange().setValues(Collections.singletonList(rowValues));
        return sheetsService.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }

    @Override
    public UpdateValuesResponse updateRow(String range, List<Object> rowValues) throws IOException {
        return updateRow(defaultSpreadsheetId, range, rowValues);
    }

    @Override
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

    @Override
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
