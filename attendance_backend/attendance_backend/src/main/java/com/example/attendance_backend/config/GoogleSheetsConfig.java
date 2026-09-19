package com.example.attendance_backend.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.util.Collections;

@Configuration
public class GoogleSheetsConfig {

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
                throw new IllegalStateException(
                        "Không tìm thấy credentials.json tại: "
                                + credentialsPath
                );
            }

            try (InputStream in = resource.getInputStream()) {

                GoogleCredentials credentials =
                        GoogleCredentials.fromStream(in)
                                .createScoped(
                                        Collections.singletonList(
                                                SheetsScopes.SPREADSHEETS
                                        )
                                );

                return new Sheets.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        new HttpCredentialsAdapter(credentials)
                )
                        .setApplicationName(applicationName)
                        .build();
            }

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Không thể khởi tạo Google Sheets Client",
                    e
            );
        }
    }
}