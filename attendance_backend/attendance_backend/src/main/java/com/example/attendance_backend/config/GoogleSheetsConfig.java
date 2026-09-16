package com.example.attendance_backend.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.util.Collections;

/**
 * Cấu hình kết nối com.google.api.services.sheets.v4.Sheets sử dụng Google Cloud Service Account.
 */
@Configuration
public class GoogleSheetsConfig {

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsConfig.class);

    @Value("${google.sheets.credentials-path:classpath:credentials.json}")
    private String credentialsPath;

    @Value("${google.sheets.application-name:FAP-Attendance-System}")
    private String applicationName;

    private final ResourceLoader resourceLoader;

    public GoogleSheetsConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public Sheets sheetsService() {
        try {
            Resource resource = resourceLoader.getResource(credentialsPath);
            if (!resource.exists()) {
                log.warn("⚠️ [GoogleSheetsConfig] Không tìm thấy file credentials tại '{}'. Vui lòng đặt file credentials.json của Google Cloud Service Account vào src/main/resources/ để kích hoạt kết nối Google Sheets Database.", credentialsPath);
                return null;
            }

            try (InputStream in = resource.getInputStream()) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                        .createScoped(Collections.singletonList(SheetsScopes.SPREADSHEETS));

                Sheets sheets = new Sheets.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        new HttpCredentialsAdapter(credentials))
                        .setApplicationName(applicationName)
                        .build();

                log.info("✅ [GoogleSheetsConfig] Khởi tạo kết nối Google Sheets Client thành công!");
                return sheets;
            }
        } catch (Exception e) {
            log.error("❌ [GoogleSheetsConfig] Lỗi khi kết nối Google Sheets API: {}", e.getMessage(), e);
            return null;
        }
    }
}
