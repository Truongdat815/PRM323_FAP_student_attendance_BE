package com.example.attendance_backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Lớp ngoại lệ nghiệp vụ tùy chỉnh cho toàn hệ thống.
 */
public class AppException extends RuntimeException {

    private final HttpStatus status;

    public AppException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public AppException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
