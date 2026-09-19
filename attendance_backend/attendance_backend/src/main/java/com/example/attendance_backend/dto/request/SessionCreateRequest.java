package com.example.attendance_backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionCreateRequest {

    @NotBlank(message = "Mã khóa học (courseId) không được để trống")
    private String courseId;

    @NotNull(message = "Tiết học (slotNumber) không được để trống")
    @Min(value = 1, message = "Tiết học phải từ 1 đến 6")
    @Max(value = 6, message = "Tiết học phải từ 1 đến 6")
    private Integer slotNumber;

    @NotNull(message = "Thời gian hết hạn (expiredAt) không được để trống")
    private LocalDateTime expiredAt;

    private String roomNetworkIp;
}
