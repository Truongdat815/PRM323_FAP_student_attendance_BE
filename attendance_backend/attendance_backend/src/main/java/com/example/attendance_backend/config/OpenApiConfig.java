package com.example.attendance_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình tài liệu Swagger UI OpenAPI với bảo mật Bearer Token (JWT).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                .info(new Info()
                        .title("Hệ thống Điểm danh Hỗ trợ FAP (FAP Attendance System) API")
                        .version("1.0.0")
                        .description("Tài liệu RESTful API hệ thống điểm danh sử dụng Spring Boot và Google Sheets Database.")
                        .contact(new Contact()
                                .name("Ngô Hoàng Trường Đạt và các cộng sự")
                                .email("datnhse192964@fpt.edu.vn")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập JWT Token nhận được từ API /api/auth/login vào đây")));
    }
}
